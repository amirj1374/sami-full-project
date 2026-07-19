package com.sami.app.licensing.service;

import com.sami.app.licensing.domain.LicenseAuditLog;
import com.sami.app.licensing.repository.LicenseAuditLogRepository;
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

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        repository.save(LicenseAuditLog.builder()
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
