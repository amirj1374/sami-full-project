package com.sami.app.licensing.service;

import com.sami.app.licensing.domain.LicenseAuditLog;
import com.sami.app.licensing.repository.LicenseAuditLogRepository;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Writes append-only audit entries (old/new snapshots) for licensing changes. */
@Service
@RequiredArgsConstructor
public class LicenseAuditService {

    private final LicenseAuditLogRepository repository;
    private final TenantContext tenantContext;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        recordForTenant(tenantContext.requireTenantId(), entityType, entityId, action, oldValues, newValues);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordForTenant(Long tenantId, String entityType, Long entityId, String action,
                                Map<String, Object> oldValues, Map<String, Object> newValues) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Trusted tenant scope is required for licensing audit");
        }
        repository.save(LicenseAuditLog.builder()
                .tenantId(tenantId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValues(oldValues == null || oldValues.isEmpty() ? null : oldValues)
                .newValues(newValues == null || newValues.isEmpty() ? null : newValues)
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .build());
    }
}
