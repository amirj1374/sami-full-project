package com.sami.app.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.repository.InventoryWarehouseRepository;
import com.sami.app.inventory.service.InventoryLedgerService;
import com.sami.app.inventory.service.InventoryWarehouseService;
import com.sami.app.organization.service.OrganizationScopeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class InventoryWarehouseTenantIsolationTest {
    @Mock TenantContext tenantContext;
    @Mock OrganizationScopeService organizationScope;
    @Mock InventoryWarehouseRepository repository;
    @Mock JdbcTemplate jdbc;
    @Mock InventoryLedgerService ledger;
    @InjectMocks InventoryWarehouseService service;

    @Test
    void listUsesTrustedTenantAndCannotReturnAnotherTenantsWarehouses() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(repository.findByTenantIdOrderByDisplayOrderAsc(41L)).thenReturn(List.of());

        assertThat(service.list()).isEmpty();
        verify(repository).findByTenantIdOrderByDisplayOrderAsc(41L);
    }
}
