package com.sami.app.purchasing;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.repository.InventoryWarehouseRepository;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.purchasing.dto.PurLookupDtos.TypeRequest;
import com.sami.app.purchasing.repository.PurApprovalRuleRepository;
import com.sami.app.purchasing.repository.PurCancelReasonRepository;
import com.sami.app.purchasing.repository.PurIdentifierTypeRepository;
import com.sami.app.purchasing.repository.PurStatusRepository;
import com.sami.app.purchasing.repository.PurTypeRepository;
import com.sami.app.purchasing.repository.PurchaseRepository;
import com.sami.app.purchasing.service.PurchasingConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchasingConfigTenantTest {

    @Mock TenantContext tenantContext;
    @Mock PurStatusRepository statuses;
    @Mock PurTypeRepository types;
    @Mock PurCancelReasonRepository reasons;
    @Mock PurIdentifierTypeRepository identifiers;
    @Mock PurApprovalRuleRepository rules;
    @Mock InventoryWarehouseRepository warehouses;
    @Mock PurchaseRepository purchases;
    @InjectMocks PurchasingConfigService service;

    @Test
    void customTypeUniquenessAndPersistenceAreTenantScoped() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(types.existsByTenantIdAndCodeIgnoreCase(41L, "local-order")).thenReturn(false);
        when(types.save(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> call.getArgument(0));

        service.createType(new TypeRequest("local-order", "سفارش محلی", null,
                "LOC", true, 20));

        verify(types).existsByTenantIdAndCodeIgnoreCase(41L, "local-order");
        ArgumentCaptor<PurType> saved = ArgumentCaptor.forClass(PurType.class);
        verify(types).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(41L);
        assertThat(saved.getValue().getName()).isEqualTo("سفارش محلی");
    }
}
