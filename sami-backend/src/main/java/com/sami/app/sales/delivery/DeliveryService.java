package com.sami.app.sales.delivery;

import com.sami.app.common.exception.*;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.contact.publicapi.ContactCustomerRoleLookup;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.inventory.publicapi.InventoryStockOperations.PartialIssueCommand;
import com.sami.app.inventory.publicapi.InventoryStockOperations.StockLine;
import com.sami.app.organization.service.OrganizationScopeService;
import com.sami.app.sales.order.*;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.sami.app.sales.delivery.DeliveryDtos.*;

@Service @RequiredArgsConstructor
public class DeliveryService {
    private final TenantContext tenants; private final OrganizationScopeService scope;
    private final ContactCustomerRoleLookup contacts; private final SalesOrderRepository orders;
    private final DeliveryRepository deliveries; private final DeliveryAuditRepository audits;
    private final InventoryStockOperations inventory;

    @Transactional public Response create(Long orderId, Request r){SalesOrder order=requireOrder(orderId);scope.requireScope(r.companyId(),r.branchId());if(order.getStatus()!=SalesOrderStatus.CONFIRMED)throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"Only confirmed orders can be delivered");contacts.requireContactIdForCustomer(order.getCustomerId());Delivery d=Delivery.builder().tenantId(order.getTenantId()).order(order).companyId(order.getCompanyId()).branchId(order.getBranchId()).customerId(order.getCustomerId()).contactId(order.getContactId()).deliveryNumber(number()).status(DeliveryStatus.DRAFT).notes(r.notes()).createdBy(CurrentActor.id()).createdByEmail(CurrentActor.email()).build();for(LineRequest x:r.lines()){SalesOrderLine ol=order.getLines().stream().filter(l->Objects.equals(l.getId(),x.orderLineId())).findFirst().orElseThrow(()->ResourceNotFoundException.of("Sales order line",x.orderLineId()));BigDecimal previous=deliveries.deliveredQuantity(order.getTenantId(),ol.getId());BigDecimal deliverable=ol.getReservedQuantity().subtract(previous).max(BigDecimal.ZERO);if(x.quantity().compareTo(deliverable)>0)throw new ApiException(ErrorCode.RESOURCE_CONFLICT,"Delivery quantity exceeds the currently reserved quantity");d.getLines().add(DeliveryLine.builder().delivery(d).orderLine(ol).tenantId(order.getTenantId()).productId(ol.getProductId()).productSku(ol.getProductSku()).productName(ol.getProductName()).orderedQuantity(ol.getQuantity()).reservedQuantity(ol.getReservedQuantity()).previouslyDeliveredQuantity(previous).deliveryQuantity(x.quantity()).backorderedQuantity(ol.getBackorderedQuantity()).build());}deliveries.saveAndFlush(d);audit(d,"CREATED",null,Map.of("orderId",orderId));return Response.from(d);}
    @Transactional(readOnly=true) public List<Response> list(Long companyId,Long branchId){scope.requireScope(companyId,branchId);return deliveries.findByTenantIdAndCompanyIdAndBranchIdOrderByCreatedAtDesc(tenants.requireTenantId(),companyId,branchId).stream().map(Response::from).toList();}
    @Transactional(readOnly=true) public Response get(Long id){return Response.from(find(id));}
    @Transactional public Response update(Long id,Request r){Delivery d=find(id);if(d.getStatus()!=DeliveryStatus.DRAFT)throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"Only draft deliveries can be edited");scope.requireScope(r.companyId(),r.branchId());SalesOrder order=requireOrder(d.getOrderId());d.getLines().clear();d.setNotes(r.notes());for(LineRequest x:r.lines()){SalesOrderLine ol=order.getLines().stream().filter(l->Objects.equals(l.getId(),x.orderLineId())).findFirst().orElseThrow(()->ResourceNotFoundException.of("Sales order line",x.orderLineId()));BigDecimal previous=deliveries.deliveredQuantity(order.getTenantId(),ol.getId());BigDecimal deliverable=ol.getReservedQuantity().subtract(previous).max(BigDecimal.ZERO);if(x.quantity().compareTo(deliverable)>0)throw new ApiException(ErrorCode.RESOURCE_CONFLICT,"Delivery quantity exceeds the currently reserved quantity");d.getLines().add(DeliveryLine.builder().delivery(d).orderLine(ol).tenantId(order.getTenantId()).productId(ol.getProductId()).productSku(ol.getProductSku()).productName(ol.getProductName()).orderedQuantity(ol.getQuantity()).reservedQuantity(ol.getReservedQuantity()).previouslyDeliveredQuantity(previous).deliveryQuantity(x.quantity()).backorderedQuantity(ol.getBackorderedQuantity()).build());}audit(d,"UPDATED",null,Map.of("orderId",d.getOrderId()));return Response.from(d);}
    @Transactional public Response confirm(Long id){Delivery d=deliveries.findByIdAndTenantIdForUpdate(id,tenants.requireTenantId()).orElseThrow(()->ResourceNotFoundException.of("Delivery",id));scope.requireScope(d.getCompanyId(),d.getBranchId());if(d.getStatus()==DeliveryStatus.CONFIRMED)return Response.from(d);if(d.getStatus()!=DeliveryStatus.DRAFT)throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"Only draft deliveries can be confirmed");if(d.getLines().isEmpty())throw new ApiException(ErrorCode.VALIDATION_FAILED,"Delivery must contain at least one line");inventory.issuePartial(new PartialIssueCommand("SALES_ORDER",d.getOrderId(),"DELIVERY",d.getId(),d.getLines().stream().map(l->new StockLine(l.getOrderLineId(),l.getProductId(),l.getDeliveryQuantity(),null,null)).toList()));d.getLines().forEach(l->{SalesOrderLine orderLine=l.getOrderLine();orderLine.setFulfilledQuantity(orderLine.getFulfilledQuantity().add(l.getDeliveryQuantity()));});d.setStatus(DeliveryStatus.CONFIRMED);d.setConfirmedAt(Instant.now());audit(d,"CONFIRMED",null,Map.of("orderId",d.getOrderId(),"lines",d.getLines().size()));return Response.from(d);}
    @Transactional public Response cancel(Long id){Delivery d=find(id);if(d.getStatus()==DeliveryStatus.CONFIRMED)throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"Confirmed delivery cannot be cancelled without an approved reversal");if(d.getStatus()==DeliveryStatus.CANCELLED)throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"Delivery is already cancelled");d.setStatus(DeliveryStatus.CANCELLED);d.setCancelledAt(Instant.now());audit(d,"CANCELLED",null,Map.of());return Response.from(d);}
    @Transactional(readOnly=true) public List<AuditResponse> history(Long id){Delivery d=find(id);return audits.findByDeliveryIdAndTenantIdOrderByOccurredAtDesc(id,d.getTenantId()).stream().map(AuditResponse::from).toList();}
    private SalesOrder requireOrder(Long id){SalesOrder o=orders.findByIdAndTenantIdForUpdate(id,tenants.requireTenantId()).orElseThrow(()->ResourceNotFoundException.of("Sales order",id));scope.requireScope(o.getCompanyId(),o.getBranchId());return o;}
    private Delivery find(Long id){return deliveries.findByIdAndTenantId(id,tenants.requireTenantId()).map(d->{scope.requireScope(d.getCompanyId(),d.getBranchId());return d;}).orElseThrow(()->ResourceNotFoundException.of("Delivery",id));}
    private void audit(Delivery d,String action,Map<String,Object> old,Map<String,Object> next){audits.save(DeliveryAudit.builder().delivery(d).tenantId(d.getTenantId()).action(action).actorId(CurrentActor.id()).actorEmail(CurrentActor.email()).oldValue(old).newValue(next).build());}
    private String number(){return "DLV-%d-%06d".formatted(Year.now(ZoneOffset.UTC).getValue(),deliveries.nextNumber());}
}
