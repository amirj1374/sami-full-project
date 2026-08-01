package com.sami.app.inventory;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.domain.InventoryWarehouse;
import com.sami.app.inventory.publicapi.InventoryStockOperations.ReceiptLine;
import com.sami.app.inventory.publicapi.InventoryStockOperations.ReservationCommand;
import com.sami.app.inventory.publicapi.InventoryStockOperations.StockLine;
import com.sami.app.inventory.publicapi.InventoryStockOperations.PurchaseReceiptCommand;
import com.sami.app.inventory.service.InventoryLedgerService;
import com.sami.app.inventory.service.InventoryStockService;
import com.sami.app.product.event.ProductStockChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryStockServiceTest {

    @Mock TenantContext tenantContext;
    @Mock InventoryLedgerService ledger;
    @InjectMocks InventoryStockService service;

    @Test
    void purchaseReceiptUsesTrustedTenantAndPreservesSourceIdentity() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        InventoryWarehouse warehouse = InventoryWarehouse.builder()
                .tenantId(41L).code("MAIN").name("Main").active(true).build();
        InventoryWarehouse persisted = spy(warehouse);
        doReturn(7L).when(persisted).getId();
        when(ledger.requireWarehouse(41L, 7L)).thenReturn(persisted);
        when(ledger.requireLocation(41L, 7L, null)).thenReturn(8L);
        when(ledger.increase(eq(41L), eq(99L), eq(7L), eq(8L),
                eq(new BigDecimal("2")), eq(new BigDecimal("120")), eq("RECEIPT"),
                eq("PURCHASE"), eq(51L), eq(61L), eq("PURCHASE-RECEIPT-71-1"),
                anyString())).thenReturn(true);

        service.receivePurchase(new PurchaseReceiptCommand(7L, 51L, 71L,
                List.of(new ReceiptLine(61L, 99L, new BigDecimal("2"),
                        new BigDecimal("120"), List.of()))));

        verify(ledger).audit(eq(41L), eq("PURCHASE_RECEIPT"), eq(71L),
                eq("POSTED"), isNull(), anyMap());
        verify(ledger).publish(eq(41L), eq("PurchaseReceiptPosted"),
                eq("PURCHASE_RECEIPT"), eq(71L), anyMap());
    }

    @Test
    void missingTenantFailsClosedBeforeStockMutation() {
        when(tenantContext.requireTenantId()).thenThrow(new ApiException(ErrorCode.UNAUTHENTICATED));

        assertThatThrownBy(() -> service.reserve(new ReservationCommand(
                1L, 2L, "SALE", 3L,
                List.of(new StockLine(4L, 5L, BigDecimal.ONE, null, null)))))
                .isInstanceOf(ApiException.class);

        verifyNoInteractions(ledger);
    }

    @Test
    void productCompatibilityAdjustmentRequiresMatchingTrustedTenant() {
        doThrow(new ApiException(ErrorCode.ACCESS_DENIED))
                .when(tenantContext).requireAccessTo(52L);

        assertThatThrownBy(() -> service.onProductStockChanged(
                new ProductStockChangedEvent(52L, 9L, 1, 3, 4L, Instant.now())))
                .isInstanceOf(ApiException.class);

        verifyNoInteractions(ledger);
    }
}
