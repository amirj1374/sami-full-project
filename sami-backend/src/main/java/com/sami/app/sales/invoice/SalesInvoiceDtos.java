package com.sami.app.sales.invoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class SalesInvoiceDtos {
    private SalesInvoiceDtos() {}
    public record LineRequest(@NotNull Long deliveryLineId,@NotNull @DecimalMin("0.001") BigDecimal quantity) {}
    public record Request(@NotNull Long orderId,@NotNull Long companyId,@NotNull Long branchId,@Size(min=3,max=3) String currency,@Size(max=2000) String notes,@NotEmpty @Valid List<LineRequest> lines) {}
    public record LineResponse(Long id,Long orderLineId,Long deliveryLineId,Long productId,String name,BigDecimal quantity,BigDecimal unitPrice,BigDecimal discount,BigDecimal lineTotal) { static LineResponse from(SalesInvoiceLine l){return new LineResponse(l.getId(),l.getOrderLineId(),l.getDeliveryLineId(),l.getProductId(),l.getProductName(),l.getQuantity(),l.getUnitPrice(),l.getDiscount(),l.getLineTotal());} }
    public record InvoiceableLine(Long deliveryId,Long deliveryLineId,Long orderLineId,Long productId,String name,BigDecimal orderedQuantity,BigDecimal deliveredQuantity,BigDecimal previouslyInvoiced,BigDecimal invoiceableQuantity,BigDecimal unitPrice,BigDecimal discount) {}
    public record Response(Long id,String number,InvoiceStatus status,Long orderId,Long deliveryId,Long companyId,Long branchId,Long customerId,Long contactId,String currency,BigDecimal subtotal,BigDecimal discountTotal,BigDecimal finalAmount,BigDecimal outstandingAmount,String receivablePostingKey,String notes,List<LineResponse> lines,Instant createdAt,Instant issuedAt,Long version) { static Response from(SalesInvoice i){Long oid=i.getOrderId()!=null?i.getOrderId():(i.getOrder()==null?null:i.getOrder().getId());return new Response(i.getId(),i.getInvoiceNumber(),i.getStatus(),oid,i.getDeliveryId(),i.getCompanyId(),i.getBranchId(),i.getCustomerId(),i.getContactId(),i.getCurrency(),i.getSubtotal(),i.getDiscountTotal(),i.getFinalAmount(),i.getStatus()==InvoiceStatus.ISSUED?i.getFinalAmount():BigDecimal.ZERO,i.getReceivablePostingKey(),i.getNotes(),i.getLines().stream().map(LineResponse::from).toList(),i.getCreatedAt(),i.getIssuedAt(),i.getVersion());} }
    public record AuditResponse(Long id,String action,Long actorId,String actorEmail,Map<String,Object> oldValue,Map<String,Object> newValue,Instant occurredAt) { static AuditResponse from(SalesInvoiceAudit a){return new AuditResponse(a.getId(),a.getAction(),a.getActorId(),a.getActorEmail(),a.getOldValue(),a.getNewValue(),a.getOccurredAt());} }
}
