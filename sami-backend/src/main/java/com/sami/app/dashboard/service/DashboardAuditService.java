package com.sami.app.dashboard.service;

import com.sami.app.dashboard.domain.DashboardAuditLog;
import com.sami.app.dashboard.event.DashboardDomainEvent;
import com.sami.app.dashboard.repository.DashboardAuditLogRepository;
import com.sami.app.security.CurrentActor;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Records the append-only dashboard audit trail and publishes the corresponding
 * {@link DashboardDomainEvent}. Both happen inside the caller's transaction, so
 * an action, its audit row and its event commit atomically.
 */
@Service
@RequiredArgsConstructor
public class DashboardAuditService {

    public static final String DASHBOARD = "DASHBOARD";
    public static final String WIDGET = "WIDGET";
    public static final String KPI = "KPI";

    private final DashboardAuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantContext tenantContext;

    /** Appends an audit entry (changed fields only) and publishes a domain event. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action, String eventType,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        auditLogRepository.save(DashboardAuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValues(oldValues == null || oldValues.isEmpty() ? null : oldValues)
                .newValues(newValues == null || newValues.isEmpty() ? null : newValues)
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .build());
        publish(eventType, entityType, entityId, newValues);
    }

    /** Paginated audit history for one entity (who changed what, old/new values). */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.sami.app.dashboard.dto.AuditEntryResponse> history(
            String entityType, Long entityId, org.springframework.data.domain.Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        entityType, entityId, pageable)
                .map(com.sami.app.dashboard.dto.AuditEntryResponse::from);
    }

    /** Publishes a domain event without an audit row (e.g. KPICalculated). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String eventType, String entityType, Long entityId,
                        Map<String, Object> payload) {
        eventPublisher.publishEvent(new DashboardDomainEvent(
                UUID.randomUUID().toString(),
                tenantContext.requireTenantId(),
                eventType,
                entityType,
                entityId,
                CurrentActor.id(),
                null,
                null,
                payload == null ? Map.of() : payload,
                java.time.Instant.now()));
    }
}
