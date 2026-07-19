package com.sami.app.dashboard.dto;

import com.sami.app.dashboard.domain.DashboardAuditLog;

import java.time.Instant;
import java.util.Map;

/** One dashboard audit entry as exposed to the UI. */
public record AuditEntryResponse(
        Long id,
        String entityType,
        Long entityId,
        String action,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        Long actorId,
        String actorEmail,
        Instant createdAt
) {
    public static AuditEntryResponse from(DashboardAuditLog a) {
        return new AuditEntryResponse(a.getId(), a.getEntityType(), a.getEntityId(), a.getAction(),
                a.getOldValues(), a.getNewValues(), a.getActorId(), a.getActorEmail(), a.getCreatedAt());
    }
}
