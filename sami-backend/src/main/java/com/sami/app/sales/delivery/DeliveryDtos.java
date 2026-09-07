package com.sami.app.sales.delivery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class DeliveryDtos {
    private DeliveryDtos() {}
    public record LineRequest(@NotNull Long orderLineId,@NotNull @DecimalMin("0.001") BigDecimal quantity) {}
    public record Request(@NotNull Long companyId,@NotNull Long branchId,@Size(max=2000) String notes,@NotEmpty @Valid List<LineRequest> lines) {}
    public record LineResponse(Long id,Long orderLineId,Long productId,String name,BigDecimal orderedQuantity,BigDecimal reservedQuantity,BigDecimal previouslyDeliveredQuantity,BigDecimal deliveryQuantity,BigDecimal backorderedQuantity,BigDecimal remainingQuantity){static LineResponse from(DeliveryLine l){Long lineId=l.getOrderLineId()!=null?l.getOrderLineId():(l.getOrderLine()==null?null:l.getOrderLine().getId());return new LineResponse(l.getId(),lineId,l.getProductId(),l.getProductName(),l.getOrderedQuantity(),l.getReservedQuantity(),l.getPreviouslyDeliveredQuantity(),l.getDeliveryQuantity(),l.getBackorderedQuantity(),l.getOrderedQuantity().subtract(l.getPreviouslyDeliveredQuantity()).subtract(l.getDeliveryQuantity()).max(BigDecimal.ZERO));}}
    public record Response(Long id,String number,DeliveryStatus status,Long orderId,Long companyId,Long branchId,Long customerId,Long contactId,String notes,List<LineResponse> lines,Instant createdAt,Instant confirmedAt,Long version){static Response from(Delivery d){Long sourceId=d.getOrderId()!=null?d.getOrderId():(d.getOrder()==null?null:d.getOrder().getId());return new Response(d.getId(),d.getDeliveryNumber(),d.getStatus(),sourceId,d.getCompanyId(),d.getBranchId(),d.getCustomerId(),d.getContactId(),d.getNotes(),d.getLines().stream().map(LineResponse::from).toList(),d.getCreatedAt(),d.getConfirmedAt(),d.getVersion());}}
    public record AuditResponse(Long id,String action,Long actorId,String actorEmail,Map<String,Object> oldValue,Map<String,Object> newValue,Instant occurredAt){static AuditResponse from(DeliveryAudit a){return new AuditResponse(a.getId(),a.getAction(),a.getActorId(),a.getActorEmail(),a.getOldValue(),a.getNewValue(),a.getOccurredAt());}}
}
