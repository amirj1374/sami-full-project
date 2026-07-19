package com.sami.app.metadata.service;

import com.sami.app.metadata.domain.MetaAuditLog;
import com.sami.app.metadata.repository.MetaAuditLogRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Append-only audit entries (old/new values, actor, IP) for metadata changes. */
@Service
@RequiredArgsConstructor
public class MetaAuditService {

    private final MetaAuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        repository.save(MetaAuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValues(oldValues == null || oldValues.isEmpty() ? null : oldValues)
                .newValues(newValues == null || newValues.isEmpty() ? null : newValues)
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .ipAddress(currentIp())
                .build());
    }

    /** Best-effort client IP from the active request, when there is one. */
    private String currentIp() {
        try {
            var attrs = (org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest().getRemoteAddr();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
