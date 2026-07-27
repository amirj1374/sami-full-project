package com.sami.app.comm.service;

import com.sami.app.comm.domain.CommAuditLog;
import com.sami.app.comm.repository.CommAuditLogRepository;
import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Audit trail for communication operations. MANDATORY propagation: an audit
 * row commits or rolls back with the change it records, never independently.
 */
@Service
@RequiredArgsConstructor
public class CommAuditService {

    public static final String MESSAGE = "Message";
    public static final String CONVERSATION = "Conversation";
    public static final String CHANNEL = "Channel";
    public static final String PROVIDER = "Provider";
    public static final String TEMPLATE = "Template";

    public static final String CREATED = "Created";
    public static final String SENT = "Sent";
    public static final String DELIVERED = "Delivered";
    public static final String READ = "Read";
    public static final String FAILED = "Failed";
    public static final String EXPIRED = "Expired";
    public static final String CANCELLED = "Cancelled";
    public static final String CLOSED = "Closed";
    public static final String UPDATED = "Updated";

    private final CommAuditLogRepository repository;
    private final TenantDefaults tenantDefaults;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        repository.save(CommAuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValues(oldValues == null || oldValues.isEmpty() ? null : oldValues)
                .newValues(newValues == null || newValues.isEmpty() ? null : newValues)
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .actorIp(clientIp())
                // Mapped field: an explicit NULL would override the column
                // DEFAULT rather than trigger it — the V17 trap.
                .tenantId(tenantDefaults.current())
                .build());
    }

    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            String forwarded = attributes.getRequest().getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
