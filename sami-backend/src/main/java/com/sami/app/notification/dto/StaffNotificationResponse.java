package com.sami.app.notification.dto;

import com.sami.app.notification.domain.StaffNotification;

import java.time.Instant;

public record StaffNotificationResponse(
        Long id,
        String type,
        String titleKey,
        String messageKey,
        String route,
        Instant readAt,
        Instant createdAt
) {
    public static StaffNotificationResponse from(StaffNotification notification) {
        return new StaffNotificationResponse(
                notification.getId(), notification.getType(), notification.getTitleKey(),
                notification.getMessageKey(), notification.getRoute(), notification.getReadAt(),
                notification.getCreatedAt());
    }
}
