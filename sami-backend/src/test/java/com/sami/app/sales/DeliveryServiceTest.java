package com.sami.app.sales;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.contact.publicapi.ContactCustomerRoleLookup;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.organization.service.OrganizationScopeService;
import com.sami.app.sales.delivery.*;
import com.sami.app.sales.delivery.DeliveryDtos.*;
import com.sami.app.sales.order.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {
    @Mock TenantContext tenants; @Mock OrganizationScopeService scope; @Mock ContactCustomerRoleLookup contacts;
    @Mock SalesOrderRepository orders; @Mock DeliveryRepository deliveries; @Mock DeliveryAuditRepository audits;
    @Mock InventoryStockOperations inventory; @InjectMocks DeliveryService service;

    @Test void draftDeliveryRequiresConfirmedOrderAndKeepsPartialQuantity(){
        SalesOrder order=order(SalesOrderStatus.CONFIRMED); SalesOrderLine line=order.getLines().getFirst();
        when(tenants.requireTenantId()).thenReturn(41L); when(orders.findByIdAndTenantIdForUpdate(91L,41L)).thenReturn(Optional.of(order)); when(contacts.requireContactIdForCustomer(9L)).thenReturn(70L); when(deliveries.deliveredQuantity(41L,12L)).thenReturn(new BigDecimal("2")); when(deliveries.nextNumber()).thenReturn(1L); when(deliveries.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        Response r=service.create(91L,new Request(7L,8L,null,List.of(new LineRequest(12L,new BigDecimal("3")))));
        assertThat(r.lines()).singleElement().satisfies(l->{assertThat(l.deliveryQuantity()).isEqualByComparingTo("3");assertThat(l.previouslyDeliveredQuantity()).isEqualByComparingTo("2");}); verify(inventory,never()).issuePartial(any());
    }

    @Test void confirmedDeliveryRetryDoesNotIssueTwice(){
        Delivery d=Delivery.builder().tenantId(41L).companyId(7L).branchId(8L).status(DeliveryStatus.CONFIRMED).deliveryNumber("DLV-2026-000001").build(); ReflectionTestUtils.setField(d,"id",44L); when(tenants.requireTenantId()).thenReturn(41L); when(deliveries.findByIdAndTenantIdForUpdate(44L,41L)).thenReturn(Optional.of(d)); Response r=service.confirm(44L); assertThat(r.status()).isEqualTo(DeliveryStatus.CONFIRMED); verify(inventory,never()).issuePartial(any());
    }

    private SalesOrder order(SalesOrderStatus status){SalesOrder o=SalesOrder.builder().tenantId(41L).companyId(7L).branchId(8L).customerId(9L).contactId(70L).status(status).build(); ReflectionTestUtils.setField(o,"id",91L); SalesOrderLine l=SalesOrderLine.builder().order(o).tenantId(41L).productId(5L).productSku("P1").productName("Phone").quantity(BigDecimal.TEN).reservedQuantity(new BigDecimal("8")).backorderedQuantity(new BigDecimal("2")).fulfilledQuantity(BigDecimal.ZERO).build(); ReflectionTestUtils.setField(l,"id",12L);o.getLines().add(l);return o;}
}
