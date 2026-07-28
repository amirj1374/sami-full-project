package com.sami.app.purchasing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.purchasing.domain.PurIdentifierType;
import com.sami.app.purchasing.domain.PurStatus;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseItem;
import com.sami.app.purchasing.domain.PurchaseReceipt;
import com.sami.app.purchasing.domain.PurchaseReceiptItem;
import com.sami.app.purchasing.domain.PurchaseUnitIdentifier;
import com.sami.app.purchasing.dto.PurchaseDtos.ReceiveRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.ReceiptResponse;
import com.sami.app.purchasing.repository.PurIdentifierTypeRepository;
import com.sami.app.purchasing.repository.PurStatusRepository;
import com.sami.app.purchasing.repository.PurchaseReceiptRepository;
import com.sami.app.purchasing.repository.PurchaseUnitIdentifierRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Purchase receiving: full, partial and multiple receipts with per-line
 * remaining-quantity enforcement and serialized-unit identifiers.
 *
 * <p>Guards: only receivable statuses (approved / partially received) accept
 * receipts — cancelled and completed purchases refuse; over-receiving is
 * impossible (service check + DB constraint); serialized lines must present
 * one unit per received piece with identifiers satisfying the item's
 * serial/IMEI requirements; identifier values are DB-unique per type, so
 * duplicate serials cannot exist. Inventory updates happen in the Inventory
 * module, reacting to the published RECEIVED/COMPLETED events.
 */
@Service
@RequiredArgsConstructor
public class PurchaseReceivingService {

    private final PurchaseService purchaseService;
    private final PurchaseReceiptRepository receiptRepository;
    private final PurStatusRepository statusRepository;
    private final PurIdentifierTypeRepository identifierTypeRepository;
    private final PurchaseUnitIdentifierRepository identifierRepository;
    private final PurchaseLogService logs;

    @Transactional(readOnly = true)
    public List<ReceiptResponse> history(Long purchaseId) {
        return receiptRepository.findByPurchaseIdOrderByCreatedAtDesc(purchaseId).stream()
                .map(ReceiptResponse::from)
                .toList();
    }

    @Transactional
    public ReceiptResponse receive(Long purchaseId, ReceiveRequest request) {
        Purchase purchase = purchaseService.findWithDetailsOrThrow(purchaseId);
        if (!purchase.getStatus().isAllowsReceiving()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "This purchase is not receivable in status '"
                            + purchase.getStatus().getName() + "'");
        }

        PurchaseReceipt receipt = PurchaseReceipt.builder()
                .purchase(purchase)
                .note(request.note())
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .build();

        for (ReceiveRequest.ReceiveLine line : request.lines()) {
            PurchaseItem item = purchase.getItems().stream()
                    .filter(i -> i.getId().equals(line.purchaseItemId()))
                    .findFirst()
                    .orElseThrow(() -> ResourceNotFoundException.of("Purchase item",
                            line.purchaseItemId()));

            if (line.quantity().compareTo(item.remainingQuantity()) > 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Cannot receive %s of '%s': only %s remaining".formatted(
                                line.quantity(), item.getProduct().getName(),
                                item.remainingQuantity()));
            }

            PurchaseReceiptItem receiptItem = PurchaseReceiptItem.builder()
                    .receipt(receipt)
                    .purchaseItem(item)
                    .quantity(line.quantity())
                    .build();
            attachIdentifiers(receiptItem, item, line);
            receipt.getItems().add(receiptItem);

            item.setReceivedQuantity(item.getReceivedQuantity().add(line.quantity()));
        }

        receiptRepository.save(receipt);

        boolean fullyReceived = purchase.getItems().stream()
                .allMatch(i -> i.remainingQuantity().compareTo(BigDecimal.ZERO) == 0);
        PurStatus target = (fullyReceived
                ? statusRepository.findByIsCompletedStateTrue()
                : statusRepository.findByIsPartialStateTrue())
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "Receiving statuses are not configured"));
        purchase.setStatus(target);

        logs.record(purchase, PurchaseLogService.RECEIVED, "Goods received",
                Map.of("lines", request.lines().size(), "complete", fullyReceived));
        if (fullyReceived) {
            logs.record(purchase, PurchaseLogService.COMPLETED, "Purchase completed", null);
        }
        return ReceiptResponse.from(receipt);
    }

    /**
     * Serialized lines: one unit entry per received piece; each unit must carry
     * identifiers whose types satisfy the item's serial/IMEI requirements.
     * Values are checked here for a clean 409 and enforced again by the DB
     * unique constraint under concurrency.
     */
    private void attachIdentifiers(PurchaseReceiptItem receiptItem, PurchaseItem item,
                                   ReceiveRequest.ReceiveLine line) {
        List<ReceiveRequest.UnitIdentifiers> units =
                line.units() == null ? List.of() : line.units();
        boolean serialized = item.isRequiresSerial() || item.isRequiresImei();

        if (serialized) {
            if (line.quantity().stripTrailingZeros().scale() > 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Serialized items must be received in whole units");
            }
            if (units.size() != line.quantity().intValue()) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "'%s' requires identifiers for each of the %s received units"
                                .formatted(item.getProduct().getName(), line.quantity()));
            }
        }

        int unitIndex = 0;
        for (ReceiveRequest.UnitIdentifiers unit : units) {
            unitIndex++;
            boolean hasSerial = false;
            boolean hasImei = false;
            for (ReceiveRequest.IdentifierValue value
                    : unit.identifiers() == null ? List.<ReceiveRequest.IdentifierValue>of()
                    : unit.identifiers()) {
                PurIdentifierType type = identifierTypeRepository.findById(value.identifierTypeId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Identifier type",
                                value.identifierTypeId()));
                String trimmed = value.value().trim();
                if (identifierRepository.existsByIdentifierTypeIdAndValue(type.getId(), trimmed)) {
                    throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                            "%s '%s' already exists in the system".formatted(type.getName(), trimmed));
                }
                receiptItem.getIdentifiers().add(PurchaseUnitIdentifier.builder()
                        .receiptItem(receiptItem)
                        .unitIndex(unitIndex)
                        .identifierType(type)
                        .value(trimmed)
                        .build());
                hasSerial |= type.isSatisfiesSerial();
                hasImei |= type.isSatisfiesImei();
            }
            if (item.isRequiresSerial() && !hasSerial) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unit %d of '%s' is missing a serial number"
                                .formatted(unitIndex, item.getProduct().getName()));
            }
            if (item.isRequiresImei() && !hasImei) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unit %d of '%s' is missing an IMEI"
                                .formatted(unitIndex, item.getProduct().getName()));
            }
        }
    }
}
