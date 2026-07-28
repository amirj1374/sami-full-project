package com.sami.app.licensing.service;

import java.util.Set;

/**
 * An immutable snapshot of what a tenant is currently entitled to: which
 * features are granted, whether the licence still grants access, and what the
 * configured expiry behaviour allows.
 */
public record Entitlement(
        Long tenantId,
        Long licenseId,
        String licenseCode,
        Set<String> enabledFeatures,
        boolean licensed,
        boolean accessGranted,
        boolean withinGrace,
        boolean writesAllowed,
        String expiryBehavior
) {

    /** Snapshot for an installation with no licence configured at all. */
    public static Entitlement unlicensed(Long tenantId) {
        return new Entitlement(tenantId, null, null, Set.of(), false, false, false, true, null);
    }
}
