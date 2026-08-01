package com.sami.app.purchasing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.repository.InventoryWarehouseRepository;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.supplier.domain.Supplier;
import com.sami.app.supplier.repository.SupplierRepository;
import com.sami.app.supplier.service.SupLogService;
import com.sami.app.purchasing.domain.PurCancelReason;
import com.sami.app.purchasing.domain.PurStatus;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseItem;
import com.sami.app.purchasing.dto.PurchaseDtos.CancelRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.ItemRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseDetailResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseFilter;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseRowResponse;
import com.sami.app.purchasing.repository.PurchaseRepository;
import com.sami.app.purchasing.repository.PurchaseSpecifications;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Purchase lifecycle: drafts, submission with configurable amount-based
 * approval, approve/reject, and cancellation with configurable reasons.
 *
 * <p>Cross-module touch points are deliberately narrow: the supplier is a
 * Supplier Management reference (blocked/blacklisted suppliers are refused at
 * submission — the flag travels with the supplier's status, not with code),
 * supplier purchase history lands in the supplier log, and everything else is
 * published as {@code PurchaseDomainEvent}s for inventory/accounting to consume.
 */
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchasingConfigService config;
    private final InventoryWarehouseRepository warehouseRepository;
    private final TenantContext tenantContext;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseNumberGenerator numberGenerator;
    private final PurchaseLogService logs;
    private final SupLogService supplierLogs;

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public Page<PurchaseRowResponse> list(PurchaseFilter filter, Pageable pageable) {
        Specification<Purchase> spec = Specification.allOf(
                PurchaseSpecifications.hasTenant(tenantContext.requireTenantId()),
                PurchaseSpecifications.globalSearch(filter.search()),
                PurchaseSpecifications.hasSupplier(filter.supplierId()),
                PurchaseSpecifications.hasStatus(filter.statusId()),
                PurchaseSpecifications.hasType(filter.typeId()),
                PurchaseSpecifications.hasWarehouse(filter.warehouseId()),
                PurchaseSpecifications.containsProduct(filter.productId()),
                PurchaseSpecifications.createdBy(filter.createdBy()),
                PurchaseSpecifications.createdOnOrAfter(filter.createdFrom()),
                PurchaseSpecifications.createdBefore(filter.createdTo()));
        return purchaseRepository.findAll(spec, pageable).map(PurchaseRowResponse::from);
    }

    @Transactional(readOnly = true)
    public PurchaseDetailResponse getDetail(Long id) {
        return PurchaseDetailResponse.from(findWithDetailsOrThrow(id));
    }

    // ---------------------------------------------------------------- drafts

    /** Creates a draft. Drafts affect nothing outside this module. */
    @Transactional
    public PurchaseDetailResponse create(PurchaseRequest request) {
        return createInternal(request, null);
    }

    @Transactional
    PurchaseDetailResponse createImported(PurchaseRequest request, String importKey) {
        return createInternal(request, importKey);
    }

    private PurchaseDetailResponse createInternal(PurchaseRequest request, String importKey) {
        PurType type = config.requireType(request.typeId());
        PurStatus draft = config.requireDraftStatus();

        Purchase purchase = Purchase.builder()
                .tenantId(tenantContext.requireTenantId())
                .purchaseNumber(numberGenerator.next(type))
                .importKey(importKey)
                .type(type)
                .status(draft)
                .supplier(findSupplierOrThrow(request.supplierId()))
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .build();
        applyHeaderAndItems(purchase, request);
        purchaseRepository.save(purchase);

        logs.record(purchase, PurchaseLogService.CREATED, "Purchase created (draft)",
                Map.of("number", purchase.getPurchaseNumber()));
        return PurchaseDetailResponse.from(purchase);
    }

    /** Edits are only possible while the status allows them (drafts). */
    @Transactional
    public PurchaseDetailResponse update(Long id, PurchaseRequest request) {
        Purchase purchase = findWithDetailsOrThrow(id);
        if (!purchase.getStatus().isAllowsEditing()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only drafts can be edited");
        }
        if (request.expectedVersion() != null
                && !request.expectedVersion().equals(purchase.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "The purchase was modified by someone else; reload and retry");
        }
        purchase.setType(config.requireType(request.typeId()));
        purchase.setSupplier(findSupplierOrThrow(request.supplierId()));
        applyHeaderAndItems(purchase, request);

        logs.record(purchase, PurchaseLogService.UPDATED, "Draft updated", null);
        return PurchaseDetailResponse.from(purchase);
    }

    /** Drafts can be discarded entirely; anything else must be cancelled. */
    @Transactional
    public void deleteDraft(Long id) {
        Purchase purchase = findWithDetailsOrThrow(id);
        if (!purchase.getStatus().isDraftState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only drafts can be deleted; cancel the purchase instead");
        }
        purchaseRepository.delete(purchase);
    }

    // -------------------------------------------------------------- workflow

    /**
     * Submits a draft. Blocked suppliers are refused here; the configurable
     * approval rules decide pending-approval vs auto-approved.
     */
    @Transactional
    public PurchaseDetailResponse submit(Long id) {
        Purchase purchase = findWithDetailsOrThrow(id);
        if (!purchase.getStatus().isDraftState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only drafts can be submitted");
        }
        Supplier supplier = purchase.getSupplier();
        if (supplier.getStatus().isBlocking()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Supplier %s is blocked (%s); purchasing from this supplier is not allowed"
                            .formatted(supplier.getDisplayName(), supplier.getStatus().getName()));
        }

        purchase.setSubmittedAt(Instant.now());
        boolean needsApproval = config.requiresApproval(purchase.getTotalAmount());

        if (needsApproval) {
            purchase.setStatus(config.requirePendingStatus());
            logs.record(purchase, PurchaseLogService.SUBMITTED, "Submitted for approval",
                    Map.of("total", purchase.getTotalAmount()));
        } else {
            approveInternal(purchase, "Auto-approved (below approval threshold)");
        }
        return PurchaseDetailResponse.from(purchase);
    }

    @Transactional
    public PurchaseDetailResponse approve(Long id) {
        Purchase purchase = findWithDetailsOrThrow(id);
        if (!purchase.getStatus().isPendingState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only purchases pending approval can be approved");
        }
        approveInternal(purchase, "Purchase approved");
        return PurchaseDetailResponse.from(purchase);
    }

    @Transactional
    public PurchaseDetailResponse reject(Long id, String note) {
        Purchase purchase = findWithDetailsOrThrow(id);
        if (!purchase.getStatus().isPendingState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only purchases pending approval can be rejected");
        }
        purchase.setStatus(config.requireRejectedStatus());
        logs.record(purchase, PurchaseLogService.REJECTED, "Purchase rejected",
                note == null || note.isBlank() ? null : Map.of("note", note));
        return PurchaseDetailResponse.from(purchase);
    }

    /** Cancellation with a configurable reason; refused once terminal. */
    @Transactional
    public PurchaseDetailResponse cancel(Long id, CancelRequest request) {
        Purchase purchase = findWithDetailsOrThrow(id);
        if (purchase.getStatus().isTerminal()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Completed, cancelled or rejected purchases cannot be cancelled");
        }
        PurCancelReason reason = config.requireCancelReason(request.reasonId());

        purchase.setStatus(config.requireCancelledStatus());
        purchase.setCancelledAt(Instant.now());
        purchase.setCancelledBy(CurrentActor.id());
        purchase.setCancelReason(reason);
        purchase.setCancelNote(request.note());

        logs.record(purchase, PurchaseLogService.CANCELLED, "Purchase cancelled",
                Map.of("reason", reason.getName()));
        return PurchaseDetailResponse.from(purchase);
    }

    // -------------------------------------------------------------- internals

    private void approveInternal(Purchase purchase, String title) {
        purchase.setStatus(config.requireApprovedStatus());
        purchase.setApprovedAt(Instant.now());
        purchase.setApprovedBy(CurrentActor.id());
        logs.record(purchase, PurchaseLogService.APPROVED, title,
                Map.of("total", purchase.getTotalAmount()));
        // Supplier history: the order appears in the supplier's log/timeline.
        supplierLogs.record(purchase.getSupplier(), "PURCHASE_ORDER",
                "Purchase order " + purchase.getPurchaseNumber() + " approved",
                Map.of("purchaseId", purchase.getId(), "number", purchase.getPurchaseNumber(),
                        "total", purchase.getTotalAmount()));
    }

    private void applyHeaderAndItems(Purchase purchase, PurchaseRequest request) {
        purchase.setWarehouse(request.warehouseId() == null ? null
                : warehouseRepository.findByIdAndTenantId(request.warehouseId(),
                                tenantContext.requireTenantId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Warehouse",
                                request.warehouseId())));
        purchase.setNotes(request.notes());

        purchase.getItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : request.items()) {
            PurchaseItem line = PurchaseItem.builder()
                    .purchase(purchase)
                    .product(productRepository.findByIdAndTenantId(item.productId(),
                                    tenantContext.requireTenantId())
                            .orElseThrow(() -> ResourceNotFoundException.of("Product",
                                    item.productId())))
                    .description(item.description())
                    .quantity(item.quantity())
                    .unit(item.unit())
                    .unitPrice(item.unitPrice())
                    .discount(item.discount() == null ? BigDecimal.ZERO : item.discount())
                    .expectedDelivery(item.expectedDelivery())
                    .requiresSerial(item.requiresSerial())
                    .requiresImei(item.requiresImei())
                    .build();
            if (line.lineTotal().compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Line discount cannot exceed the line amount");
            }
            purchase.getItems().add(line);
            total = total.add(line.lineTotal());
        }
        purchase.setTotalAmount(total);
    }

    private Supplier findSupplierOrThrow(Long supplierId) {
        return supplierRepository.findByIdAndTenantId(supplierId, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", supplierId));
    }

    Purchase findWithDetailsOrThrow(Long id) {
        return purchaseRepository.findWithDetailsByIdAndTenantId(
                        id, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Purchase", id));
    }

    Purchase findWithDetailsForUpdateOrThrow(Long id) {
        return purchaseRepository.findForUpdate(id, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Purchase", id));
    }
}
