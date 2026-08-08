package com.sami.app.purchasing.dto;

import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseAttachment;
import com.sami.app.purchasing.domain.PurchaseItem;
import com.sami.app.purchasing.domain.PurchaseLog;
import com.sami.app.purchasing.domain.PurchaseReceipt;
import com.sami.app.purchasing.domain.PurchaseReturn;
import com.sami.app.purchasing.domain.PurchaseSellerType;
import com.sami.app.purchasing.domain.PurchaseSettlementStatus;
import com.sami.app.purchasing.domain.PurchaseItemCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request/response payloads for purchase documents. */
public final class PurchaseDtos {

    private PurchaseDtos() {
    }

    // ------------------------------------------------------------- responses

    public record PurchaseRowResponse(
            Long id, String purchaseNumber,
            PurLookupDtos.TypeResponse type,
            PurLookupDtos.StatusResponse status,
            PurchaseSellerType sellerType,
            Long sellerId, String sellerCode, String sellerName,
            Long supplierId, String supplierCode, String supplierName,
            Long companyId, Long branchId, Long linkedSaleId,
            PurLookupDtos.WarehouseResponse warehouse,
            BigDecimal totalAmount,
            String createdByEmail,
            Instant createdAt, Instant updatedAt, Long version
    ) {
        public static PurchaseRowResponse from(Purchase p) {
            return new PurchaseRowResponse(
                    p.getId(), p.getPurchaseNumber(),
                    PurLookupDtos.TypeResponse.from(p.getType()),
                    PurLookupDtos.StatusResponse.from(p.getStatus()),
                    p.getSellerType(),
                    p.getSupplier() != null ? p.getSupplier().getId() : p.getSellerCustomer().getId(),
                    p.getSupplier() != null ? p.getSupplier().getSupplierCode() : p.getSellerCustomer().getCustomerCode(),
                    p.getSupplier() != null ? p.getSupplier().getDisplayName() : p.getSellerCustomer().getDisplayName(),
                    p.getSupplier() != null ? p.getSupplier().getId() : p.getSellerCustomer().getId(),
                    p.getSupplier() != null ? p.getSupplier().getSupplierCode() : p.getSellerCustomer().getCustomerCode(),
                    p.getSupplier() != null ? p.getSupplier().getDisplayName() : p.getSellerCustomer().getDisplayName(),
                    p.getCompanyId(), p.getBranchId(), p.getLinkedSaleId(),
                    p.getWarehouse() != null
                            ? PurLookupDtos.WarehouseResponse.from(p.getWarehouse()) : null,
                    p.getTotalAmount(),
                    p.getCreatedByEmail(),
                    p.getCreatedAt(), p.getUpdatedAt(), p.getVersion());
        }
    }

    public record ItemResponse(
            Long id, Long productId, String productName, String productSku,
            String description, BigDecimal quantity, String unit,
            BigDecimal unitPrice, BigDecimal discount, BigDecimal lineTotal,
            LocalDate expectedDelivery, boolean requiresSerial, boolean requiresImei,
            BigDecimal receivedQuantity, BigDecimal returnedQuantity, BigDecimal remainingQuantity
    ) {
        public static ItemResponse from(PurchaseItem i) {
            return new ItemResponse(i.getId(), i.getProduct().getId(),
                    i.getProduct().getName(), i.getProduct().getSku(),
                    i.getDescription(), i.getQuantity(), i.getUnit(),
                    i.getUnitPrice(), i.getDiscount(), i.lineTotal(),
                    i.getExpectedDelivery(), i.isRequiresSerial(), i.isRequiresImei(),
                    i.getReceivedQuantity(), i.getReturnedQuantity(), i.remainingQuantity());
        }
    }

    public record PurchaseDetailResponse(
            PurchaseRowResponse purchase,
            String notes,
            PurchaseItemCondition itemCondition, String inspectionNotes,
            boolean ownershipDeclared, String declarationNotes, BigDecimal valuationAmount,
            PurchaseSettlementStatus settlementStatus, String settlementMethod,
            String settlementReference, BigDecimal settledAmount, Instant settledAt,
            List<ItemResponse> items,
            Instant submittedAt, Instant approvedAt, Long approvedBy,
            Instant cancelledAt, String cancelReason, String cancelNote
    ) {
        public static PurchaseDetailResponse from(Purchase p) {
            return new PurchaseDetailResponse(
                    PurchaseRowResponse.from(p),
                    p.getNotes(),
                    p.getItemCondition(), p.getInspectionNotes(), p.isOwnershipDeclared(),
                    p.getDeclarationNotes(), p.getValuationAmount(), p.getSettlementStatus(),
                    p.getSettlementMethod(), p.getSettlementReference(), p.getSettledAmount(), p.getSettledAt(),
                    p.getItems().stream().map(ItemResponse::from).toList(),
                    p.getSubmittedAt(), p.getApprovedAt(), p.getApprovedBy(),
                    p.getCancelledAt(),
                    p.getCancelReason() != null ? p.getCancelReason().getName() : null,
                    p.getCancelNote());
        }
    }

    public record ReceiptResponse(
            Long id, String note, String createdByEmail, Instant createdAt,
            List<ReceiptLineResponse> lines
    ) {
        public record ReceiptLineResponse(Long purchaseItemId, BigDecimal quantity,
                                          List<IdentifierResponse> identifiers) {
        }

        public record IdentifierResponse(int unitIndex, String type, String value) {
        }

        public static ReceiptResponse from(PurchaseReceipt r) {
            return new ReceiptResponse(r.getId(), r.getNote(), r.getCreatedByEmail(),
                    r.getCreatedAt(),
                    r.getItems().stream().map(line -> new ReceiptLineResponse(
                            line.getPurchaseItem().getId(),
                            line.getQuantity(),
                            line.getIdentifiers().stream().map(ident -> new IdentifierResponse(
                                    ident.getUnitIndex(),
                                    ident.getIdentifierType().getName(),
                                    ident.getValue())).toList())).toList());
        }
    }

    public record ReturnResponse(
            Long id, String reason, String createdByEmail, Instant createdAt,
            List<ReturnLineResponse> lines
    ) {
        public record ReturnLineResponse(Long purchaseItemId, BigDecimal quantity) {
        }

        public static ReturnResponse from(PurchaseReturn r) {
            return new ReturnResponse(r.getId(), r.getReason(), r.getCreatedByEmail(),
                    r.getCreatedAt(),
                    r.getItems().stream().map(line -> new ReturnLineResponse(
                            line.getPurchaseItem().getId(), line.getQuantity())).toList());
        }
    }

    public record LogResponse(Long id, String action, String title,
                              Map<String, Object> detail, String actorEmail, Instant occurredAt) {
        public static LogResponse from(PurchaseLog l) {
            return new LogResponse(l.getId(), l.getAction(), l.getTitle(), l.getDetail(),
                    l.getActorEmail(), l.getOccurredAt());
        }
    }

    public record AttachmentResponse(Long id, String fileName, String contentType,
                                     long fileSize, String uploadedByEmail, Instant createdAt) {
        public static AttachmentResponse from(PurchaseAttachment a) {
            return new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType(),
                    a.getFileSize(), a.getUploadedByEmail(), a.getCreatedAt());
        }
    }

    public record DashboardResponse(
            long totalPurchases,
            long drafts,
            long pendingApproval,
            long openReceipts,
            long completed,
            BigDecimal committedAmount,
            BigDecimal receivedAmount,
            long overdueLines
    ) {
    }

    public record ReportGroup(String key, String label, long count, BigDecimal amount) {
    }

    public record OverdueLineResponse(
            Long purchaseId, String purchaseNumber, String supplierName,
            Long itemId, String productName, LocalDate expectedDelivery,
            BigDecimal orderedQuantity, BigDecimal remainingQuantity
    ) {
    }

    public record ReportResponse(
            List<ReportGroup> byStatus,
            List<ReportGroup> byType,
            List<ReportGroup> bySupplier,
            List<OverdueLineResponse> overdueLines
    ) {
    }

    public record ImportResponse(int created, int skipped, List<String> purchaseNumbers) {
    }

    // -------------------------------------------------------------- requests

    public record ItemRequest(
            @NotNull(message = "Product is required") Long productId,
            @Size(max = 500) String description,
            @NotNull @Positive(message = "Quantity must be positive") BigDecimal quantity,
            @NotBlank @Size(max = 32) String unit,
            @NotNull @DecimalMin(value = "0.00") BigDecimal unitPrice,
            @DecimalMin(value = "0.00") BigDecimal discount,
            LocalDate expectedDelivery,
            boolean requiresSerial,
            boolean requiresImei
    ) {
    }

    /** Create/update payload; items are a full replacement (drafts only). */
    public record PurchaseRequest(
            @NotNull(message = "Purchase type is required") Long typeId,
            PurchaseSellerType sellerType,
            Long supplierId,
            Long sellerCustomerId,
            Long companyId,
            Long branchId,
            Long linkedSaleId,
            PurchaseItemCondition itemCondition,
            @Size(max = 2000) String inspectionNotes,
            boolean ownershipDeclared,
            @Size(max = 1000) String declarationNotes,
            @DecimalMin(value = "0.00") BigDecimal valuationAmount,
            PurchaseSettlementStatus settlementStatus,
            @Size(max = 40) String settlementMethod,
            @Size(max = 160) String settlementReference,
            @DecimalMin(value = "0.00") BigDecimal settledAmount,
            Long warehouseId,
            @Size(max = 2000) String notes,
            @NotEmpty(message = "At least one item is required") @Valid List<ItemRequest> items,
            Long expectedVersion
    ) {
    }

    public record CancelRequest(
            @NotNull(message = "Cancellation reason is required") Long reasonId,
            @Size(max = 500) String note
    ) {
    }

    public record ReceiveRequest(
            @Size(max = 500) String note,
            @NotEmpty(message = "At least one line is required") @Valid List<ReceiveLine> lines
    ) {
        public record ReceiveLine(
                @NotNull Long purchaseItemId,
                @NotNull @Positive BigDecimal quantity,
                /** One entry per serialized unit; empty for bulk items. */
                @Valid List<UnitIdentifiers> units
        ) {
        }

        public record UnitIdentifiers(
                @Valid List<IdentifierValue> identifiers,
                @Size(max = 128) String hamtaActivationCode
        ) {
        }

        public record IdentifierValue(
                @NotNull Long identifierTypeId,
                @NotBlank @Size(max = 128) String value
        ) {
        }
    }

    public record ReturnRequest(
            @NotBlank(message = "Return reason is required") @Size(max = 500) String reason,
            @NotEmpty(message = "At least one line is required") @Valid List<ReturnLine> lines
    ) {
        public record ReturnLine(
                @NotNull Long purchaseItemId,
                @NotNull @Positive BigDecimal quantity
        ) {
        }
    }

    /** Combinable listing filters; all optional. */
    public record PurchaseFilter(
            String search,
            Long supplierId,
            Long statusId,
            Long typeId,
            Long warehouseId,
            Long productId,
            Long createdBy,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
    }
}
