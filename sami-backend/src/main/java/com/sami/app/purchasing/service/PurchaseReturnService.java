package com.sami.app.purchasing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.inventory.publicapi.InventoryStockOperations.StockLine;
import com.sami.app.inventory.publicapi.InventoryStockOperations.SupplierReturnCommand;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseItem;
import com.sami.app.purchasing.domain.PurchaseReturn;
import com.sami.app.purchasing.domain.PurchaseReturnItem;
import com.sami.app.purchasing.dto.PurchaseDtos.ReturnRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.ReturnResponse;
import com.sami.app.purchasing.repository.PurchaseReturnRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Supplier returns: partial or complete, multiple per purchase, fully
 * traceable. A line can only return what was received and not yet returned;
 * inventory reacts to the published RETURNED event.
 */
@Service
@RequiredArgsConstructor
public class PurchaseReturnService {

    private final PurchaseService purchaseService;
    private final PurchaseReturnRepository returnRepository;
    private final PurchaseLogService logs;
    private final InventoryStockOperations inventory;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<ReturnResponse> history(Long purchaseId) {
        purchaseService.findWithDetailsOrThrow(purchaseId);
        return returnRepository.findByPurchaseIdAndTenantIdOrderByCreatedAtDesc(
                        purchaseId, tenantContext.requireTenantId()).stream()
                .map(ReturnResponse::from)
                .toList();
    }

    @Transactional
    public ReturnResponse returnToSupplier(Long purchaseId, ReturnRequest request) {
        Purchase purchase = purchaseService.findWithDetailsForUpdateOrThrow(purchaseId);
        if (purchase.getStatus().isCancelledState() || purchase.getStatus().isRejectedState()
                || purchase.getStatus().isDraftState() || purchase.getStatus().isPendingState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only received purchases can have supplier returns");
        }

        PurchaseReturn purchaseReturn = PurchaseReturn.builder()
                .tenantId(tenantContext.requireTenantId())
                .purchase(purchase)
                .reason(request.reason().trim())
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .build();

        for (ReturnRequest.ReturnLine line : request.lines()) {
            PurchaseItem item = purchase.getItems().stream()
                    .filter(i -> i.getId().equals(line.purchaseItemId()))
                    .findFirst()
                    .orElseThrow(() -> ResourceNotFoundException.of("Purchase item",
                            line.purchaseItemId()));

            BigDecimal returnable = item.getReceivedQuantity().subtract(item.getReturnedQuantity());
            if (line.quantity().compareTo(returnable) > 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Cannot return %s of '%s': only %s returnable".formatted(
                                line.quantity(), item.getProduct().getName(), returnable));
            }

            purchaseReturn.getItems().add(PurchaseReturnItem.builder()
                    .purchaseReturn(purchaseReturn)
                    .purchaseItem(item)
                    .quantity(line.quantity())
                    .build());
            item.setReturnedQuantity(item.getReturnedQuantity().add(line.quantity()));
        }

        returnRepository.saveAndFlush(purchaseReturn);
        inventory.returnToSupplier(new SupplierReturnCommand(
                purchase.getWarehouse() == null ? null : purchase.getWarehouse().getId(),
                purchase.getId(), purchaseReturn.getId(), purchaseReturn.getItems().stream()
                        .map(item -> new StockLine(
                                item.getPurchaseItem().getId(),
                                item.getPurchaseItem().getProduct().getId(),
                                item.getQuantity(), null, null))
                        .toList()));
        logs.record(purchase, PurchaseLogService.RETURNED, "Returned to supplier",
                Map.of("reason", request.reason(), "lines", request.lines().size()));
        return ReturnResponse.from(purchaseReturn);
    }
}
