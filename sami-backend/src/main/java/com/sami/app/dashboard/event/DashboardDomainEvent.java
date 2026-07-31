package com.sami.app.dashboard.event;

import java.time.Instant;
import java.util.Map;

/**
 * The dashboard module's published business event (DashboardCreated,
 * DashboardUpdated, WidgetAdded, DashboardShared, KPICalculated,
 * ThresholdExceeded, …). Carries the metadata the spec requires so downstream
 * consumers (notifications, AI insights, external BI) stay fully decoupled.
 *
 * <p>Subscribe with a plain Spring listener:
 * <pre>
 * &#64;TransactionalEventListener
 * public void on(DashboardDomainEvent e) {
 *     if ("ThresholdExceeded".equals(e.eventType())) { ...notify... }
 * }
 * </pre>
 *
 * @param eventId    unique event identifier
 * @param eventType  the domain event name
 * @param entityType DASHBOARD | WIDGET | KPI
 * @param entityId   affected entity id
 * @param userId     acting user (null for system)
 * @param companyId  optional company scope
 * @param branchId   optional branch scope
 * @param payload    event-specific data
 * @param occurredAt event timestamp
 */
public record DashboardDomainEvent(
        String eventId,
        Long tenantId,
        String eventType,
        String entityType,
        Long entityId,
        Long userId,
        Long companyId,
        Long branchId,
        Map<String, Object> payload,
        Instant occurredAt
) {
    // Event type constants (the published domain events).
    public static final String DASHBOARD_CREATED = "DashboardCreated";
    public static final String DASHBOARD_UPDATED = "DashboardUpdated";
    public static final String DASHBOARD_DELETED = "DashboardDeleted";
    public static final String DASHBOARD_SHARED = "DashboardShared";
    public static final String WIDGET_ADDED = "WidgetAdded";
    public static final String WIDGET_UPDATED = "WidgetUpdated";
    public static final String WIDGET_REMOVED = "WidgetRemoved";
    public static final String KPI_CREATED = "KPICreated";
    public static final String KPI_MODIFIED = "KPIModified";
    public static final String KPI_CALCULATED = "KPICalculated";
    public static final String THRESHOLD_EXCEEDED = "ThresholdExceeded";
}
