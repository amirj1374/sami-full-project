package com.sami.app.sales;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.contact.publicapi.ContactCustomerRoleLookup;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.organization.service.OrganizationScopeService;
import com.sami.app.product.domain.Product;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.sales.order.*;
import com.sami.app.sales.document.SalesDocumentType;
import com.sami.app.sales.order.SalesOrderDtos.*;
import com.sami.app.sales.publicapi.QuotationConversionSource;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {
    @Mock TenantContext tenants; @Mock OrganizationScopeService scope; @Mock ContactCustomerRoleLookup contacts;
    @Mock ProductRepository products; @Mock SalesOrderRepository orders; @Mock SalesOrderAuditRepository audits;
    @Mock InventoryStockOperations inventory; @Mock QuotationConversionSource quotations;
    @InjectMocks SalesOrderService service;

    @Test void directOrderKeepsRegisteredPriceSnapshot() {
        when(tenants.requireTenantId()).thenReturn(41L); when(contacts.requireContactIdForCustomer(9L)).thenReturn(70L);
        Product p=Product.builder().tenantId(41L).name("Phone").sku("P1").price(new BigDecimal("999")).active(true).build(); ReflectionTestUtils.setField(p,"id",5L);
        when(products.findByIdAndTenantId(5L,41L)).thenReturn(Optional.of(p)); when(orders.nextNumber()).thenReturn(1L);
        when(orders.saveAndFlush(any())).thenAnswer(i->{SalesOrder o=i.getArgument(0);ReflectionTestUtils.setField(o,"id",91L);return o;});
        Response r=service.create(new Request(7L,8L,9L,"IRR",null,List.of(new LineRequest(5L,new BigDecimal("2"),new BigDecimal("100"),new BigDecimal("20"))),null));
        assertThat(r.finalAmount()).isEqualByComparingTo("180.00"); assertThat(r.lines()).singleElement().satisfies(l->assertThat(l.unitPrice()).isEqualByComparingTo("100.00")); verify(inventory,never()).reserveAvailable(any());
    }

    @Test void issuedQuotationConversionCopiesSnapshotWithoutRepricing() {
        when(quotations.requireIssuedQuotation(22L)).thenReturn(new QuotationConversionSource.QuotationSnapshot(22L,SalesDocumentType.QUOTATION,41L,7L,8L,9L,70L,"IRR",List.of(new QuotationConversionSource.Line(3L,5L,"P1","Phone",BigDecimal.TWO,new BigDecimal("100"),BigDecimal.TEN,new BigDecimal("190")))));
        when(orders.nextNumber()).thenReturn(1L); when(orders.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        Response r=service.convert(22L);
        assertThat(r.sourceDocumentId()).isEqualTo(22L); assertThat(r.lines()).singleElement().satisfies(l->assertThat(l.unitPrice()).isEqualByComparingTo("100")); verify(products,never()).findByIdAndTenantId(anyLong(),anyLong());
    }

    @Test void confirmationPersistsPartialReservationAsBackorderWithoutIssue() {
        SalesOrder o=SalesOrder.builder().tenantId(41L).companyId(7L).branchId(8L).customerId(9L).contactId(70L).orderNumber("ORD-2026-000001").status(SalesOrderStatus.DRAFT).build(); SalesOrderLine line=SalesOrderLine.builder().order(o).tenantId(41L).productId(5L).productSku("P1").productName("Phone").quantity(BigDecimal.TEN).unitPrice(BigDecimal.ONE).discount(BigDecimal.ZERO).lineTotal(BigDecimal.TEN).build(); ReflectionTestUtils.setField(o,"id",91L); ReflectionTestUtils.setField(line,"id",12L); o.getLines().add(line);
        when(tenants.requireTenantId()).thenReturn(41L); when(orders.findByIdAndTenantId(91L,41L)).thenReturn(Optional.of(o)); when(inventory.reserveAvailable(any())).thenReturn(new InventoryStockOperations.ReservationResult(List.of(new InventoryStockOperations.ReservationAllocation(12L,BigDecimal.TEN,new BigDecimal("4"),new BigDecimal("6")))));
        Response r=service.confirm(91L); assertThat(r.status()).isEqualTo(SalesOrderStatus.CONFIRMED); assertThat(r.lines()).singleElement().satisfies(l->assertThat(l.backorderedQuantity()).isEqualByComparingTo("6")); verify(inventory,never()).issue(anyString(),anyLong());
    }
}
