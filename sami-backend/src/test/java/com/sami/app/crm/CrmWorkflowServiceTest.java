package com.sami.app.crm;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.domain.CrmFollowUpTask;
import com.sami.app.crm.repository.CrmFollowUpTaskRepository;
import com.sami.app.crm.repository.CrmLeadRepository;
import com.sami.app.crm.repository.CrmOpportunityRepository;
import com.sami.app.crm.service.CrmWorkflowService;
import com.sami.app.crm.service.CustomerService;
import com.sami.app.crm.dto.CrmWorkflowDtos.FollowUpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmWorkflowServiceTest {
    @Mock TenantContext tenantContext;
    @Mock CustomerService customers;
    @Mock CrmLeadRepository leads;
    @Mock CrmOpportunityRepository opportunities;
    @Mock CrmFollowUpTaskRepository tasks;
    @InjectMocks CrmWorkflowService service;

    @Test
    void followUpCreationIsIdempotentWithinTenant() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        var existing = new CrmFollowUpTask();
        when(tasks.findByTenantIdAndIdempotencyKey(41L, "sale-9-satisfaction"))
                .thenReturn(Optional.of(existing));

        var result = service.createTask(new FollowUpRequest(null, null, null, null,
                Instant.parse("2026-10-01T10:00:00Z"), "sale-9-satisfaction"));

        assertThat(result).isSameAs(existing);
        verify(tasks, never()).save(any());
    }
}
