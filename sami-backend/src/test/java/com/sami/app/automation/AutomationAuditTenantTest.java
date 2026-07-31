package com.sami.app.automation;

import com.sami.app.automation.domain.AutomationAuditLog;
import com.sami.app.automation.repository.AutomationAuditLogRepository;
import com.sami.app.automation.service.AutomationAuditService;
import com.sami.app.common.tenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AutomationAuditTenantTest {

    @Test
    void auditUsesTrustedTenantContext() {
        AutomationAuditLogRepository repository = mock(AutomationAuditLogRepository.class);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.requireTenantId()).thenReturn(51L);
        AutomationAuditService service = new AutomationAuditService(repository, tenantContext);

        service.record("RULE", 7L, "UPDATED", Map.of("name", "before"), Map.of("name", "after"));

        ArgumentCaptor<AutomationAuditLog> entry = ArgumentCaptor.forClass(AutomationAuditLog.class);
        verify(repository).save(entry.capture());
        assertEquals(51L, entry.getValue().getTenantId());
    }
}
