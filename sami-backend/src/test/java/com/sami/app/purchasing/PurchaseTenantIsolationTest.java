package com.sami.app.purchasing;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.repository.InventoryWarehouseRepository;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.purchasing.repository.PurchaseRepository;
import com.sami.app.purchasing.service.PurchaseLogService;
import com.sami.app.purchasing.service.PurchaseNumberGenerator;
import com.sami.app.purchasing.service.PurchaseService;
import com.sami.app.purchasing.service.PurchasingConfigService;
import com.sami.app.supplier.repository.SupplierRepository;
import com.sami.app.supplier.service.SupLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseTenantIsolationTest {

    @Mock PurchaseRepository purchases;
    @Mock PurchasingConfigService config;
    @Mock InventoryWarehouseRepository warehouses;
    @Mock TenantContext tenantContext;
    @Mock SupplierRepository suppliers;
    @Mock ProductRepository products;
    @Mock PurchaseNumberGenerator numbers;
    @Mock PurchaseLogService logs;
    @Mock SupLogService supplierLogs;
    @InjectMocks PurchaseService service;

    @Test
    void detailLookupUsesTrustedTenantAndHidesAnotherTenantsPurchase() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(purchases.findWithDetailsByIdAndTenantId(9L, 41L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(9L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(purchases).findWithDetailsByIdAndTenantId(9L, 41L);
    }
}
