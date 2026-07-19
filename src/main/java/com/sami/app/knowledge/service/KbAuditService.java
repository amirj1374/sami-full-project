package com.sami.app.knowledge.service;

import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.knowledge.domain.KbAuditLog;
import com.sami.app.knowledge.repository.KbAuditLogRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/** Audit trail for knowledge operations, including actor IP. */
@Service
@RequiredArgsConstructor
public class KbAuditService {

    public static final String CREATED = "Created";
    public static final String UPDATED = "Updated";
    public static final String APPROVED = "Approved";
    public static final String REJECTED = "Rejected";
    public static final String PUBLISHED = "Published";
    public static final String VIEWED = "Viewed";
    public static final String EXPORTED = "Exported";
    public static final String ARCHIVED = "Archived";
    public static final String DEPRECATED = "Deprecated";

    private final KbAuditLogRepository repository;
    private final TenantDefaults tenantDefaults;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        repository.save(KbAuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValues(oldValues == null || oldValues.isEmpty() ? null : oldValues)
                .newValues(newValues == null || newValues.isEmpty() ? null : newValues)
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .actorIp(clientIp())
                
                // tenant_id is mapped, so Hibernate always sends it: an explicit
                // NULL would override the column DEFAULT instead of triggering it.
                .tenantId(tenantDefaults.current())
                .build());
    }

    private String clientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return attrs.getRequest().getRemoteAddr();
    }
}
