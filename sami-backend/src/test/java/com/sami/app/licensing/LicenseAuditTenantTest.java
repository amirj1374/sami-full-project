package com.sami.app.licensing;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.licensing.domain.LicenseAuditLog;
import com.sami.app.licensing.repository.LicenseAuditLogRepository;
import com.sami.app.licensing.service.LicenseAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LicenseAuditTenantTest {

    @Test
    void auditUsesTrustedTenantContext() {
        LicenseAuditLogRepository repository = mock(LicenseAuditLogRepository.class);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.requireTenantId()).thenReturn(73L);
        LicenseAuditService service = new LicenseAuditService(repository, tenantContext);

        service.record("LICENSE", 5L, "UPDATED", null, Map.of("status", "active"));

        ArgumentCaptor<LicenseAuditLog> captor = ArgumentCaptor.forClass(LicenseAuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(73L);
        assertThat(captor.getValue().getEntityId()).isEqualTo(5L);
    }

    @Test
    void backgroundAuditKeepsPersistedTenant() {
        LicenseAuditLogRepository repository = mock(LicenseAuditLogRepository.class);
        LicenseAuditService service = new LicenseAuditService(repository, mock(TenantContext.class));

        service.recordForTenant(91L, "LICENSE", 6L, "EXPIRED", null, Map.of());

        ArgumentCaptor<LicenseAuditLog> captor = ArgumentCaptor.forClass(LicenseAuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(91L);
    }
}
