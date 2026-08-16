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
import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.service.CustomerEventService;
import com.sami.app.sales.domain.Sale;
import com.sami.app.sales.domain.SaleStatus;
import com.sami.app.sales.repository.SaleRepository;
import com.sami.app.purchasing.domain.PurCancelReason;
import com.sami.app.purchasing.domain.PurStatus;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseItem;
import com.sami.app.purchasing.domain.PurchaseSellerType;
import com.sami.app.purchasing.domain.PurchaseSettlementStatus;
import com.sami.app.purchasing.dto.PurchaseDtos.CancelRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.ItemRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseDetailResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseFilter;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseRowResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.SettlementRequest;
import com.sami.app.purchasing.repository.PurchaseRepository;
import com.sami.app.purchasing.repository.PurchaseSpecifications;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

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
    private final CustomerRepository customerRepository;
    private final CustomerEventService customerEvents;
    private final SaleRepository saleRepository;
    private final JdbcTemplate jdbc;

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
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .build();
        applyHeaderAndItems(purchase, request);
        purchaseRepository.save(purchase);

        logs.record(purchase, PurchaseLogService.CREATED, "Purchase created (draft)",
                Map.of("number", purchase.getPurchaseNumber()));
        recordCustomerEvent(purchase, "CUSTOMER_PURCHASE_CREATED", "Customer purchase created");
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
        if (supplier != null && supplier.getStatus().isBlocking()) {
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
        recordCustomerEvent(purchase, "CUSTOMER_PURCHASE_SUBMITTED", "Customer purchase submitted");
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

    /**
     * Resolves the settlement gate for an approved customer-origin purchase.
     * This remains separate from draft editing so a purchase cannot become
     * permanently unreceivable after it enters the approval workflow.
     */
    @Transactional
    public PurchaseDetailResponse updateSettlement(Long id, SettlementRequest request) {
        Purchase purchase = findWithDetailsForUpdateOrThrow(id);
        if (purchase.getSellerType() != PurchaseSellerType.CUSTOMER) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Settlement is only available for customer-origin purchases");
        }
        if (!purchase.getStatus().isAllowsReceiving()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Settlement can only be recorded for a receivable purchase");
        }
        if (!request.expectedVersion().equals(purchase.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "The purchase was modified by someone else; reload and retry");
        }
        if (request.status() == PurchaseSettlementStatus.PENDING) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Choose settled or explicitly waived before receiving goods");
        }

        purchase.setSettlementStatus(request.status());
        if (request.status() == PurchaseSettlementStatus.SETTLED) {
            purchase.setSettlementMethod(trimToNull(request.method()));
            purchase.setSettlementReference(trimToNull(request.reference()));
            purchase.setSettledAmount(request.amount());
            purchase.setSettledAt(Instant.now());
        } else {
            purchase.setSettlementMethod(null);
            purchase.setSettlementReference(null);
            purchase.setSettledAmount(null);
            purchase.setSettledAt(null);
        }

        Map<String, Object> detail = new java.util.LinkedHashMap<>();
        detail.put("status", request.status().name());
        if (request.amount() != null && request.status() == PurchaseSettlementStatus.SETTLED) {
            detail.put("amount", request.amount());
        }
        logs.record(purchase, PurchaseLogService.SETTLEMENT_UPDATED,
                "Customer purchase settlement updated", detail);
        recordCustomerEvent(purchase, "CUSTOMER_PURCHASE_SETTLEMENT_UPDATED",
                "Customer purchase settlement updated");
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
        recordCustomerEvent(purchase, "CUSTOMER_PURCHASE_CANCELLED", "Customer purchase cancelled");
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
        if (purchase.getSupplier() != null) {
            supplierLogs.record(purchase.getSupplier(), "PURCHASE_ORDER",
                    "Purchase order " + purchase.getPurchaseNumber() + " approved",
                    Map.of("purchaseId", purchase.getId(), "number", purchase.getPurchaseNumber(),
                            "total", purchase.getTotalAmount()));
        } else {
            recordCustomerEvent(purchase, "CUSTOMER_PURCHASE_APPROVED", "Customer purchase approved");
        }
    }

    private void applyHeaderAndItems(Purchase purchase, PurchaseRequest request) {
        applyCounterparty(purchase, request);
        Long tenantId = tenantContext.requireTenantId();
        purchase.setCompanyId(resolveCompanyId(request.companyId(), tenantId));
        purchase.setBranchId(resolveBranchId(request.branchId(), purchase.getCompanyId(), tenantId));
        purchase.setLinkedSaleId(validateLinkedSale(request.linkedSaleId(), purchase));
        purchase.setItemCondition(request.itemCondition() == null
                ? com.sami.app.purchasing.domain.PurchaseItemCondition.OTHER : request.itemCondition());
        purchase.setInspectionNotes(request.inspectionNotes());
        purchase.setOwnershipDeclared(request.ownershipDeclared());
        purchase.setDeclarationNotes(request.declarationNotes());
        purchase.setValuationAmount(request.valuationAmount());
        purchase.setSettlementStatus(request.settlementStatus() == null
                ? PurchaseSettlementStatus.PENDING : request.settlementStatus());
        purchase.setSettlementMethod(request.settlementMethod());
        purchase.setSettlementReference(request.settlementReference());
        purchase.setSettledAmount(request.settledAmount());
        purchase.setSettledAt(purchase.getSettlementStatus() == PurchaseSettlementStatus.SETTLED
                ? (purchase.getSettledAt() == null ? Instant.now() : purchase.getSettledAt()) : null);
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

    private void applyCounterparty(Purchase purchase, PurchaseRequest request) {
        PurchaseSellerType sellerType = request.sellerType() == null
                ? PurchaseSellerType.SUPPLIER : request.sellerType();
        purchase.setSellerType(sellerType);
        if (sellerType == PurchaseSellerType.SUPPLIER) {
            if (request.supplierId() == null) throw new ApiException(ErrorCode.VALIDATION_FAILED, "Supplier is required");
            purchase.setSupplier(findSupplierOrThrow(request.supplierId()));
            purchase.setSellerCustomer(null);
        } else {
            if (request.sellerCustomerId() == null) throw new ApiException(ErrorCode.VALIDATION_FAILED, "Customer seller is required");
            Customer customer = customerRepository.findByIdAndTenantId(request.sellerCustomerId(), tenantContext.requireTenantId())
                    .filter(c -> c.getDeletedAt() == null && c.getMergedInto() == null)
                    .orElseThrow(() -> ResourceNotFoundException.of("Customer", request.sellerCustomerId()));
            purchase.setSupplier(null);
            purchase.setSellerCustomer(customer);
        }
    }

    private Long resolveCompanyId(Long requestedId, Long tenantId) {
        if (requestedId != null) {
            Integer count = jdbc.queryForObject("select count(*) from companies where id=? and tenant_id=?", Integer.class, requestedId, tenantId);
            if (count != null && count == 1) return requestedId;
            throw ResourceNotFoundException.of("Company", requestedId);
        }
        Long id = jdbc.query("select id from companies where tenant_id=? order by id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, tenantId);
        if (id == null) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Configure a company before creating purchases");
        return id;
    }

    private Long resolveBranchId(Long requestedId, Long companyId, Long tenantId) {
        if (requestedId != null) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from branches where id=? and company_id=? and tenant_id=?",
                    Integer.class, requestedId, companyId, tenantId);
            if (count != null && count == 1) return requestedId;
            throw ResourceNotFoundException.of("Branch", requestedId);
        }
        Long id = jdbc.query("select id from branches where company_id=? and tenant_id=? order by id limit 1",
                rs -> rs.next() ? rs.getLong(1) : null, companyId, tenantId);
        if (id == null) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Configure a branch before creating purchases");
        return id;
    }

    private Long validateLinkedSale(Long saleId, Purchase purchase) {
        if (saleId == null) return null;
        if (purchase.getSellerType() != PurchaseSellerType.CUSTOMER) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Only customer-origin purchases can link a sale");
        }
        Sale sale = saleRepository.findByIdAndTenantId(saleId, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Sale", saleId));
        if (!sale.getCustomerId().equals(purchase.getSellerCustomer().getId()) || sale.getStatus() == SaleStatus.CANCELLED) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Linked sale must belong to the same customer and must not be cancelled");
        }
        if (!sale.getCompanyId().equals(purchase.getCompanyId()) || !sale.getBranchId().equals(purchase.getBranchId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Linked sale must use the same company and branch");
        }
        return saleId;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void recordCustomerEvent(Purchase purchase, String type, String title) {
        if (purchase.getSellerCustomer() != null) {
            customerEvents.record(purchase.getSellerCustomer().getId(), type, title,
                    Map.of("purchaseId", purchase.getId(), "number", purchase.getPurchaseNumber()),
                    "purchasing");
        }
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
