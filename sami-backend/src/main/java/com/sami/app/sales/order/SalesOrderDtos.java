package com.sami.app.sales.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class SalesOrderDtos {
    private SalesOrderDtos() {}
    public record LineRequest(@NotNull Long productId, @NotNull @DecimalMin("0.001") BigDecimal quantity,
                              @NotNull @DecimalMin("0") BigDecimal unitPrice, @DecimalMin("0") BigDecimal discount) {}
    public record Request(@NotNull Long companyId, @NotNull Long branchId, @NotNull Long customerId,
                          @Size(min=3,max=3) String currency, @Size(max=2000) String notes,
                          @NotEmpty @Valid List<LineRequest> lines, Long expectedVersion) {}
    public record LineResponse(Long id, Long sourceLineId, Long productId, String sku, String name,
                               BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal lineTotal,
                               BigDecimal reservedQuantity, BigDecimal backorderedQuantity, BigDecimal fulfilledQuantity) {
        static LineResponse from(SalesOrderLine l) { return new LineResponse(l.getId(),l.getSourceLineId(),l.getProductId(),l.getProductSku(),l.getProductName(),l.getQuantity(),l.getUnitPrice(),l.getDiscount(),l.getLineTotal(),l.getReservedQuantity(),l.getBackorderedQuantity(),l.getFulfilledQuantity()); }
    }
    public record Response(Long id, String number, SalesOrderStatus status, Long companyId, Long branchId,
                           Long customerId, Long contactId, Long sourceDocumentId, String currency,
                           BigDecimal subtotal, BigDecimal discountTotal, BigDecimal finalAmount, String notes,
                           List<LineResponse> lines, Instant createdAt, Instant confirmedAt, Long version) {
        static Response from(SalesOrder o) { return new Response(o.getId(),o.getOrderNumber(),o.getStatus(),o.getCompanyId(),o.getBranchId(),o.getCustomerId(),o.getContactId(),o.getSourceDocumentId(),o.getCurrency(),o.getSubtotal(),o.getDiscountTotal(),o.getFinalAmount(),o.getNotes(),o.getLines().stream().map(LineResponse::from).toList(),o.getCreatedAt(),o.getConfirmedAt(),o.getVersion()); }
    }
    public record AuditResponse(Long id,String action,Long actorId,String actorEmail,Map<String,Object> oldValue,Map<String,Object> newValue,Instant occurredAt) { static AuditResponse from(SalesOrderAudit a){return new AuditResponse(a.getId(),a.getAction(),a.getActorId(),a.getActorEmail(),a.getOldValue(),a.getNewValue(),a.getOccurredAt());} }
}
