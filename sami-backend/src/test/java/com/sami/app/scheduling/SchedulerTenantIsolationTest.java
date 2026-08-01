package com.sami.app.scheduling;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.scheduler.repository.JobStatusRepository;
import com.sami.app.common.scheduler.repository.ScheduledJobRepository;
import com.sami.app.common.scheduler.service.JobService;
import com.sami.app.common.scheduler.service.ScheduleCalculator;
import com.sami.app.common.scheduler.spi.JobHandlerRegistry;
import com.sami.app.common.tenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerTenantIsolationTest {

    @Mock ScheduledJobRepository jobs;
    @Mock JobStatusRepository statuses;
    @Mock JobHandlerRegistry handlers;
    @Mock ScheduleCalculator calculator;
    @Mock TenantContext tenantContext;
    @InjectMocks JobService service;

    @Test
    void listUsesOnlyTheTrustedTenant() {
        when(tenantContext.requireTenantId()).thenReturn(42L);
        when(jobs.findAllByTenantIdOrderByCodeAsc(42L)).thenReturn(List.of());

        service.list();

        verify(jobs).findAllByTenantIdOrderByCodeAsc(42L);
    }

    @Test
    void getHidesAnotherTenantsJob() {
        when(tenantContext.requireTenantId()).thenReturn(42L);
        when(jobs.findByIdAndTenantId(7L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(7L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobs).findByIdAndTenantId(7L, 42L);
    }
}
