package com.sami.app.licensing.event;

import java.time.Instant;
import java.util.Map;

/**
 * Published on licence, subscription, tenant, feature and usage transitions so
 * downstream consumers (notifications, automation rules, billing, analytics)
 * react without the licensing core knowing about them.
 */
public record LicenseDomainEvent(
        String eventId,
        String eventType,
        Long tenantId,
        Long licenseId,
        String licenseCode,
        Map<String, Object> payload,
        Instant occurredAt
) {
    public static final String TENANT_CREATED = "TenantCreated";
    public static final String TENANT_SUSPENDED = "TenantSuspended";
    public static final String LICENSE_ACTIVATED = "LicenseActivated";
    public static final String LICENSE_EXPIRED = "LicenseExpired";
    public static final String LICENSE_SUSPENDED = "LicenseSuspended";
    public static final String SUBSCRIPTION_RENEWED = "SubscriptionRenewed";
    public static final String SUBSCRIPTION_EXPIRED = "SubscriptionExpired";
    public static final String FEATURE_ENABLED = "FeatureEnabled";
    public static final String FEATURE_DISABLED = "FeatureDisabled";
    public static final String USAGE_LIMIT_EXCEEDED = "UsageLimitExceeded";
}
