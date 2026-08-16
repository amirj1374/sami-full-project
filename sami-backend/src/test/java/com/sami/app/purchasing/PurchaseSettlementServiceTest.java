package com.sami.app.purchasing;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.service.CustomerEventService;
import com.sami.app.inventory.repository.InventoryWarehouseRepository;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.purchasing.domain.PurStatus;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseItemCondition;
import com.sami.app.purchasing.domain.PurchaseSellerType;
import com.sami.app.purchasing.domain.PurchaseSettlementStatus;
import com.sami.app.purchasing.dto.PurchaseDtos.SettlementRequest;
import com.sami.app.purchasing.repository.PurchaseRepository;
import com.sami.app.purchasing.service.PurchaseLogService;
import com.sami.app.purchasing.service.PurchaseNumberGenerator;
import com.sami.app.purchasing.service.PurchaseService;
import com.sami.app.purchasing.service.PurchasingConfigService;
import com.sami.app.sales.repository.SaleRepository;
import com.sami.app.supplier.repository.SupplierRepository;
import com.sami.app.supplier.service.SupLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseSettlementServiceTest {

    @Mock PurchaseRepository purchases;
    @Mock PurchasingConfigService config;
    @Mock InventoryWarehouseRepository warehouses;
    @Mock TenantContext tenantContext;
    @Mock SupplierRepository suppliers;
    @Mock ProductRepository products;
    @Mock PurchaseNumberGenerator numbers;
    @Mock PurchaseLogService logs;
    @Mock SupLogService supplierLogs;
    @Mock CustomerRepository customers;
    @Mock CustomerEventService customerEvents;
    @Mock SaleRepository sales;
    @Mock JdbcTemplate jdbc;
    @InjectMocks PurchaseService service;

    @Test
    void approvedCustomerPurchaseCanResolvePendingSettlement() {
        Purchase purchase = customerPurchase();
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(purchases.findForUpdate(7L, 41L)).thenReturn(Optional.of(purchase));

        service.updateSettlement(7L, new SettlementRequest(
                PurchaseSettlementStatus.SETTLED, "CARD", "REF-12",
                new BigDecimal("125000"), 3L));

        assertThat(purchase.getSettlementStatus()).isEqualTo(PurchaseSettlementStatus.SETTLED);
        assertThat(purchase.getSettlementMethod()).isEqualTo("CARD");
        assertThat(purchase.getSettlementReference()).isEqualTo("REF-12");
        assertThat(purchase.getSettledAmount()).isEqualByComparingTo("125000");
        assertThat(purchase.getSettledAt()).isNotNull();
        verify(logs).record(eq(purchase), eq(PurchaseLogService.SETTLEMENT_UPDATED),
                eq("Customer purchase settlement updated"), any(Map.class));
        verify(customerEvents).record(eq(11L), eq("CUSTOMER_PURCHASE_SETTLEMENT_UPDATED"),
                eq("Customer purchase settlement updated"), any(Map.class), eq("purchasing"));
    }

    @Test
    void waivedSettlementClearsPaymentDetails() {
        Purchase purchase = customerPurchase();
        purchase.setSettlementMethod("CASH");
        purchase.setSettlementReference("OLD");
        purchase.setSettledAmount(BigDecimal.TEN);
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(purchases.findForUpdate(7L, 41L)).thenReturn(Optional.of(purchase));

        service.updateSettlement(7L, new SettlementRequest(
                PurchaseSettlementStatus.WAIVED, "IGNORED", "IGNORED", BigDecimal.TEN, 3L));

        assertThat(purchase.getSettlementStatus()).isEqualTo(PurchaseSettlementStatus.WAIVED);
        assertThat(purchase.getSettlementMethod()).isNull();
        assertThat(purchase.getSettlementReference()).isNull();
        assertThat(purchase.getSettledAmount()).isNull();
        assertThat(purchase.getSettledAt()).isNull();
    }

    @Test
    void staleSettlementUpdateIsRejected() {
        Purchase purchase = customerPurchase();
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(purchases.findForUpdate(7L, 41L)).thenReturn(Optional.of(purchase));

        assertThatThrownBy(() -> service.updateSettlement(7L, new SettlementRequest(
                PurchaseSettlementStatus.SETTLED, null, null, null, 2L)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("modified by someone else");

        verify(logs, never()).record(any(), any(), any(), any());
    }

    private Purchase customerPurchase() {
        Customer customer = Customer.builder()
                .tenantId(41L)
                .customerCode("CUS-11")
                .displayName("Customer 11")
                .build();
        ReflectionTestUtils.setField(customer, "id", 11L);
        Purchase purchase = Purchase.builder()
                .tenantId(41L)
                .purchaseNumber("PUR-7")
                .type(PurType.builder().code("BUY").name("Purchase").build())
                .status(PurStatus.builder().code("APPROVED").name("Approved")
                        .allowsReceiving(true).build())
                .sellerType(PurchaseSellerType.CUSTOMER)
                .sellerCustomer(customer)
                .companyId(2L)
                .branchId(3L)
                .itemCondition(PurchaseItemCondition.USED)
                .settlementStatus(PurchaseSettlementStatus.PENDING)
                .totalAmount(new BigDecimal("125000"))
                .build();
        ReflectionTestUtils.setField(purchase, "id", 7L);
        ReflectionTestUtils.setField(purchase, "version", 3L);
        return purchase;
    }
}
