package com.sami.app.scheduling.service;

import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.scheduling.domain.ScheduleAuditLog;
import com.sami.app.scheduling.repository.ScheduleAuditLogRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Audit trail for scheduling operations.
 *
 * <p>{@code Propagation.MANDATORY} is deliberate: an audit row must be written
 * in the same transaction as the change it records, so a rolled-back booking
 * cannot leave a phantom audit entry claiming it happened.
 */
@Service
@RequiredArgsConstructor
public class SchedulingAuditService {

    public static final String SCHEDULE = "Schedule";
    public static final String RESERVATION = "Reservation";
    public static final String RESOURCE = "Resource";
    public static final String WAITING_LIST = "WaitingListEntry";

    public static final String CREATED = "Created";
    public static final String UPDATED = "Updated";
    public static final String CONFIRMED = "Confirmed";
    public static final String CANCELLED = "Cancelled";
    public static final String RESCHEDULED = "Rescheduled";
    public static final String RESERVED = "Reserved";
    public static final String RELEASED = "Released";
    public static final String CHECKED_IN = "CheckedIn";
    public static final String CHECKED_OUT = "CheckedOut";
    public static final String NO_SHOW = "NoShow";
    public static final String PROMOTED = "Promoted";

    private final ScheduleAuditLogRepository repository;
    private final TenantDefaults tenantDefaults;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        repository.save(ScheduleAuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValues(oldValues == null || oldValues.isEmpty() ? null : oldValues)
                .newValues(newValues == null || newValues.isEmpty() ? null : newValues)
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .actorIp(clientIp())
                // tenant_id is a mapped field, so Hibernate always sends it and
                // an explicit NULL would override the column DEFAULT rather
                // than trigger it — the V17 trap.
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
