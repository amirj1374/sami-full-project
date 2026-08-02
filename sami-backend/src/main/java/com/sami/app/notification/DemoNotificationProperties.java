package com.sami.app.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Deployment-level kill switch for customer-demo notifications. */
@ConfigurationProperties(prefix = "app.notifications.demo")
public record DemoNotificationProperties(boolean enabled) {
}
