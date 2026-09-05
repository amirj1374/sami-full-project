package com.sami.app.automation.provider;

import com.sami.app.automation.spi.ActionContext;
import com.sami.app.automation.spi.ActionProvider;
import com.sami.app.automation.spi.ActionResult;
import com.sami.app.notification.service.StaffNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sends a notification. There is no general notification port in the ERP yet, so
 * this implementation is the future-ready seam (mirroring {@code LoggingMailSender}):
 * it validates and records the notification intent and returns its payload. A
 * real channel (email/SMS/push) drops in behind the same action type with no rule
 * or engine change. Config: {@code {"channel","recipient","subject","body"}}.
 */
@Slf4j
@Component @RequiredArgsConstructor
public class NotificationActionProvider implements ActionProvider {
    private final StaffNotificationService notifications;

    @Override
    public String type() {
        return "notify";
    }

    @Override
    public String label() {
        return "Send notification";
    }

    @Override
    public ActionResult execute(ActionContext context) {
        String recipient = context.configString("recipient");
        Long userId = "actor".equalsIgnoreCase(recipient) ? context.trigger().actorId() : parseUserId(recipient);
        if (userId == null) return ActionResult.fail("notify: recipient must be 'actor' or a user id");
        String titleKey = value(context, "titleKey", "notifications.automation.title");
        String messageKey = value(context, "messageKey", "notifications.automation.message");
        String route = value(context, "route", "/");
        String source = context.trigger().triggerType() + ":" + context.trigger().entityType() + ":"
                + context.trigger().entityId() + ":" + context.trigger().occurredAt();
        notifications.createSystem(userId, "AUTOMATION", titleKey, messageKey, route,
                "automation:" + Integer.toUnsignedString(source.hashCode()) + ":" + userId + ":" + messageKey);
        log.info("[automation] notification center → user {}", userId);
        return ActionResult.ok("Notification created",
                Map.of("channel", "notification-center", "userId", userId));
    }

    @Override
    public String validate(Map<String, Object> config) {
        Object recipient = config.get("recipient");
        return (recipient == null || recipient.toString().isBlank())
                ? "notify: 'recipient' is required" : null;
    }

    private String value(ActionContext context, String key, String fallback) {
        String value = context.configString(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private Long parseUserId(String value) {
        if (value == null) return null;
        try { return Long.valueOf(value); } catch (NumberFormatException ignored) { return null; }
    }
}
