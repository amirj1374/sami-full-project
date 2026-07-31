package com.sami.app.sales.event;
import java.time.Instant; import java.util.Map;
public record SaleDomainEvent(Long tenantId,Long companyId,Long branchId,Long saleId,String invoiceNumber,String action,Map<String,Object> detail,Long actorId,Instant occurredAt){}
