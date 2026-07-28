package com.sami.app.files.service;

import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.files.domain.FileAuditLog;
import com.sami.app.files.repository.FileAuditLogRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Audit trail for file operations. Captures the actor IP, which the platform
 * spec requires and which no module except metadata recorded before this one.
 */
@Service
@RequiredArgsConstructor
public class FileAuditService {

    public static final String ACTION_UPLOADED = "Uploaded";
    public static final String ACTION_DOWNLOADED = "Downloaded";
    public static final String ACTION_UPDATED = "Updated";
    public static final String ACTION_DELETED = "Deleted";
    public static final String ACTION_RESTORED = "Restored";
    public static final String ACTION_VERSION_CREATED = "VersionCreated";
    public static final String ACTION_ROLLED_BACK = "RolledBack";
    public static final String ACTION_STORAGE_CHANGED = "StorageChanged";
    public static final String ACTION_RETENTION_APPLIED = "RetentionApplied";
    public static final String ACTION_QUARANTINED = "Quarantined";
    public static final String ACTION_MOVED = "Moved";
    public static final String ACTION_COPIED = "Copied";

    private final FileAuditLogRepository repository;
    private final TenantDefaults tenantDefaults;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        repository.save(FileAuditLog.builder()
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

    /**
     * Best-effort client IP. Honours {@code X-Forwarded-For}'s first hop, since
     * the production topology puts nginx in front of the backend.
     */
    static String clientIp() {
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
