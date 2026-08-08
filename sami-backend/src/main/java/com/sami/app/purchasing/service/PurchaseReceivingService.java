package com.sami.app.purchasing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.hamta.HamtaService;
import com.sami.app.inventory.publicapi.InventoryStockOperations.PurchaseReceiptCommand;
import com.sami.app.inventory.publicapi.InventoryStockOperations.ReceiptLine;
import com.sami.app.inventory.publicapi.InventoryStockOperations.SerialIdentity;
import com.sami.app.purchasing.domain.PurIdentifierType;
import com.sami.app.purchasing.domain.PurStatus;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseItem;
import com.sami.app.purchasing.domain.PurchaseReceipt;
import com.sami.app.purchasing.domain.PurchaseReceiptItem;
import com.sami.app.purchasing.domain.PurchaseUnitIdentifier;
import com.sami.app.purchasing.domain.PurchaseSellerType;
import com.sami.app.purchasing.domain.PurchaseSettlementStatus;
import com.sami.app.purchasing.dto.PurchaseDtos.ReceiveRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.ReceiptResponse;
import com.sami.app.purchasing.repository.PurchaseReceiptRepository;
import com.sami.app.purchasing.repository.PurchaseUnitIdentifierRepository;
import com.sami.app.security.CurrentActor;
import com.sami.app.crm.service.CustomerEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final PurchasingConfigService config;
    private final PurchaseUnitIdentifierRepository identifierRepository;
    private final TenantContext tenantContext;
    private final PurchaseLogService logs;
    private final InventoryStockOperations inventory;
    private final CustomerEventService customerEvents;
    private final HamtaService hamtaService;

    @Transactional(readOnly = true)
    public List<ReceiptResponse> history(Long purchaseId) {
        purchaseService.findWithDetailsOrThrow(purchaseId);
        Long tenantId = tenantContext.requireTenantId();
        List<PurchaseReceipt> receipts = receiptRepository
                .findByPurchaseIdAndTenantIdOrderByCreatedAtDesc(purchaseId, tenantId);
        Map<Long, List<PurchaseUnitIdentifier>> identifiersByReceiptItem = identifierRepository
                .findByReceiptItemReceiptIdInAndTenantIdOrderByReceiptItemIdAscUnitIndexAscIdAsc(
                        receipts.stream().map(PurchaseReceipt::getId).toList(), tenantId)
                .stream()
                .collect(Collectors.groupingBy(identifier -> identifier.getReceiptItem().getId(),
                        LinkedHashMap::new, Collectors.toList()));
        return receipts.stream()
                .map(receipt -> toResponse(receipt, identifiersByReceiptItem))
                .toList();
    }

    private ReceiptResponse toResponse(PurchaseReceipt receipt,
                                       Map<Long, List<PurchaseUnitIdentifier>> identifiersByReceiptItem) {
        return new ReceiptResponse(receipt.getId(), receipt.getNote(), receipt.getCreatedByEmail(),
                receipt.getCreatedAt(), receipt.getItems().stream().map(item ->
                        new ReceiptResponse.ReceiptLineResponse(item.getPurchaseItem().getId(), item.getQuantity(),
                                identifiersByReceiptItem.getOrDefault(item.getId(), List.of()).stream().map(identifier ->
                                        new ReceiptResponse.IdentifierResponse(identifier.getUnitIndex(),
                                                identifier.getIdentifierType().getName(), identifier.getValue()))
                                        .toList()))
                        .toList());
    }

    @Transactional
    public ReceiptResponse receive(Long purchaseId, ReceiveRequest request) {
        Purchase purchase = purchaseService.findWithDetailsForUpdateOrThrow(purchaseId);
        if (!purchase.getStatus().isAllowsReceiving()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "This purchase is not receivable in status '"
                            + purchase.getStatus().getName() + "'");
        }
        if (purchase.getSellerType() == PurchaseSellerType.CUSTOMER
                && purchase.getSettlementStatus() == PurchaseSettlementStatus.PENDING) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Customer-origin purchases must be settled or explicitly waived before receipt");
        }

        PurchaseReceipt receipt = PurchaseReceipt.builder()
                .tenantId(tenantContext.requireTenantId())
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

        receiptRepository.saveAndFlush(receipt);

        inventory.receivePurchase(new PurchaseReceiptCommand(
                purchase.getWarehouse() == null ? null : purchase.getWarehouse().getId(),
                purchase.getId(), receipt.getId(), receipt.getItems().stream()
                        .map(item -> new ReceiptLine(
                                item.getPurchaseItem().getId(),
                                item.getPurchaseItem().getProduct().getId(),
                                item.getQuantity(),
                                item.getPurchaseItem().getUnitPrice(),
                                serials(item, request.lines().stream()
                                        .filter(line -> line.purchaseItemId().equals(item.getPurchaseItem().getId()))
                                        .findFirst().orElseThrow())))
                        .toList()));

        boolean fullyReceived = purchase.getItems().stream()
                .allMatch(i -> i.remainingQuantity().compareTo(BigDecimal.ZERO) == 0);
        PurStatus target = fullyReceived
                ? config.requireCompletedStatus() : config.requirePartialStatus();
        purchase.setStatus(target);

        logs.record(purchase, PurchaseLogService.RECEIVED, "Goods received",
                Map.of("lines", request.lines().size(), "complete", fullyReceived));
        if (fullyReceived) {
            logs.record(purchase, PurchaseLogService.COMPLETED, "Purchase completed", null);
        }
        if (purchase.getSellerCustomer() != null) {
            customerEvents.record(purchase.getSellerCustomer().getId(), "CUSTOMER_PURCHASE_RECEIVED",
                    "Customer purchase received",
                    Map.of("purchaseId", purchase.getId(), "receiptId", receipt.getId(), "complete", fullyReceived),
                    "purchasing");
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
        boolean hamtaRequired = hamtaService.required(item.getProduct().getId(),
                item.getPurchase().getItemCondition());

        if (hamtaRequired && !item.isRequiresImei()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "HAMTA-eligible used phones must require an IMEI");
        }

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
            if (hamtaRequired && (unit.hamtaActivationCode() == null
                    || unit.hamtaActivationCode().isBlank())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unit %d of '%s' requires a HAMTA activation code"
                                .formatted(unitIndex, item.getProduct().getName()));
            }
            boolean hasSerial = false;
            boolean hasImei = false;
            for (ReceiveRequest.IdentifierValue value
                    : unit.identifiers() == null ? List.<ReceiveRequest.IdentifierValue>of()
                    : unit.identifiers()) {
                PurIdentifierType type = config.requireIdentifierType(value.identifierTypeId());
                String trimmed = value.value().trim();
                if (type.isSatisfiesImei() && !isValidImei(trimmed)) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED,
                            "IMEI must contain 15 digits and pass the Luhn checksum");
                }
                if (identifierRepository.existsByTenantIdAndIdentifierTypeIdAndValue(
                        tenantContext.requireTenantId(), type.getId(), trimmed)) {
                    throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                            "%s '%s' already exists in the system".formatted(type.getName(), trimmed));
                }
                receiptItem.getIdentifiers().add(PurchaseUnitIdentifier.builder()
                        .tenantId(tenantContext.requireTenantId())
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

    private List<SerialIdentity> serials(PurchaseReceiptItem item, ReceiveRequest.ReceiveLine line) {
        Map<Integer, String> serials = new LinkedHashMap<>();
        Map<Integer, String> imeis = new LinkedHashMap<>();
        item.getIdentifiers().forEach(identifier -> {
            if (identifier.getIdentifierType().isSatisfiesSerial()) {
                serials.put(identifier.getUnitIndex(), identifier.getValue());
            }
            if (identifier.getIdentifierType().isSatisfiesImei()) {
                imeis.put(identifier.getUnitIndex(), identifier.getValue());
            }
        });
        java.util.LinkedHashSet<Integer> indexes = new java.util.LinkedHashSet<>();
        indexes.addAll(serials.keySet());
        indexes.addAll(imeis.keySet());
        List<SerialIdentity> result = new ArrayList<>();
        indexes.forEach(index -> {
            String code = line.units() != null && line.units().size() >= index
                    ? line.units().get(index - 1).hamtaActivationCode() : null;
            result.add(new SerialIdentity(serials.get(index), imeis.get(index), code));
        });
        return result;
    }

    public static boolean isValidImei(String value) {
        if (value == null || !value.matches("\\d{15}")) return false;
        int sum = 0;
        for (int i = 0; i < value.length(); i++) {
            int digit = value.charAt(i) - '0';
            if (i % 2 == 1) {
                digit *= 2;
                digit = digit / 10 + digit % 10;
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }
}
