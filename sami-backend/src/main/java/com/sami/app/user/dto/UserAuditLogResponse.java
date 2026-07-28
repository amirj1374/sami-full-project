package com.sami.app.user.dto;

import com.sami.app.user.domain.UserAuditLog;

import java.time.Instant;
import java.util.Map;

/** One audit entry as exposed to the admin UI. */
public record UserAuditLogResponse(
        Long id,
        Long userId,
        String userEmail,
        String action,
        Long actorId,
        String actorEmail,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        Instant createdAt
) {

    public static UserAuditLogResponse from(UserAuditLog entry) {
        return new UserAuditLogResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getUserEmail(),
                entry.getAction(),
                entry.getActorId(),
                entry.getActorEmail(),
                entry.getOldValues(),
                entry.getNewValues(),
                entry.getCreatedAt()
        );
    }
}
