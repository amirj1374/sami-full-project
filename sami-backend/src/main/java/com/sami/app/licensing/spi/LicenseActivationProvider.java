package com.sami.app.licensing.spi;

import java.util.Map;

/**
 * Extension point for licence activation/validation modes. The shipped modes are
 * {@code OFFLINE} (self-contained key verification, for air-gapped on-premise
 * installs) and {@code EMERGENCY} (a time-boxed grant when validation is
 * impossible). An {@code ONLINE} provider that calls a licence server or
 * marketplace registers the same way — the core never changes.
 */
public interface LicenseActivationProvider {

    /** MANUAL | ONLINE | OFFLINE | EMERGENCY, matching {@code licenses.activation_mode}. */
    String mode();

    String label();

    /**
     * Verifies that the presented key/fingerprint may activate.
     *
     * @param licenseKey  the key being activated
     * @param fingerprint installation identity (nullable)
     * @param context     free-form extra data (e.g. an emergency code)
     */
    ActivationDecision verify(String licenseKey, String fingerprint, Map<String, Object> context);

    /** Outcome of an activation attempt. */
    record ActivationDecision(boolean allowed, String reason, Integer grantDays) {
        public static ActivationDecision allow() {
            return new ActivationDecision(true, null, null);
        }

        public static ActivationDecision allow(int grantDays) {
            return new ActivationDecision(true, null, grantDays);
        }

        public static ActivationDecision deny(String reason) {
            return new ActivationDecision(false, reason, null);
        }
    }
}
