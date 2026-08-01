package com.sami.app.crm;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.service.CrmStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmStatsTenantTest {

    @Mock CustomerRepository customers;
    @Mock TenantContext tenantContext;
    @InjectMocks CrmStatsService service;

    @Test
    void everyReportingAggregateUsesTrustedTenant() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(customers.countByTenantIdAndMergedIntoIsNull(41L)).thenReturn(12L);
        when(customers.countByTenantIdAndCreatedAtAfterAndMergedIntoIsNull(any(), any(Instant.class))).thenReturn(3L);
        when(customers.countByStatus(41L))
                .thenReturn(java.util.Collections.singletonList(new Object[] {"Active", 12L}));
        when(customers.countByType(41L)).thenReturn(List.of());
        when(customers.countBySource(41L)).thenReturn(List.of());

        var result = service.stats();

        assertThat(result.total()).isEqualTo(12L);
        assertThat(result.newLast30Days()).isEqualTo(3L);
        verify(customers).countByTenantIdAndMergedIntoIsNull(41L);
        verify(customers).countByStatus(41L);
        verify(customers).countByType(41L);
        verify(customers).countBySource(41L);
    }
}
