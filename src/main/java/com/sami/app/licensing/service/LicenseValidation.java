package com.sami.app.licensing.service;

import java.time.Instant;
import java.util.Set;

/**
 * Outcome of validating a licence key — the public answer other systems and the
 * UI consume (never the entity itself).
 */
public record LicenseValidation(
        boolean valid,
        String reason,
        String licenseCode,
        String status,
        String planCode,
        Long tenantId,
        Instant expirationDate,
        boolean withinGrace,
        Set<String> features
) {
    public static LicenseValidation invalid(String reason) {
        return new LicenseValidation(false, reason, null, null, null, null, null, false, Set.of());
    }
}
