package com.sami.app.automation.service;

import com.sami.app.automation.domain.AutomationAuditLog;
import com.sami.app.automation.repository.AutomationAuditLogRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Writes append-only audit entries (old/new snapshots) for automation changes. */
@Service
@RequiredArgsConstructor
public class AutomationAuditService {

    private final AutomationAuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        repository.save(AutomationAuditLog.builder()
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
