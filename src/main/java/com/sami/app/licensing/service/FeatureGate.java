package com.sami.app.licensing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The contract every business module uses to gate licensed functionality —
 * mirroring the existing {@code @authz} bean so it reads the same way:
 *
 * <pre>
 *   &#64;PreAuthorize("@authz.has('inventory:view') and @features.enabled('inventory.advanced')")
 * </pre>
 *
 * Business modules therefore contain no licensing logic at all: they ask, this
 * module decides. Checks are dynamic (no restart needed to enable/disable).
 */
@Component("features")
@RequiredArgsConstructor
public class FeatureGate {

    private final EntitlementService entitlements;

    /** True when the feature is available for the current (implicit) tenant. */
    public boolean enabled(String featureCode) {
        return entitlements.isFeatureEnabled(featureCode, null);
    }

    /** True when the feature is available for an explicit tenant. */
    public boolean enabledFor(String featureCode, Long tenantId) {
        return entitlements.isFeatureEnabled(featureCode, tenantId);
    }

    /** False when a lapsed licence has put the system into a read-only mode. */
    public boolean writesAllowed() {
        return entitlements.resolve(null).writesAllowed();
    }
}
