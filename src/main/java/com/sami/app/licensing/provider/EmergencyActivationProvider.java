package com.sami.app.licensing.provider;

import com.sami.app.licensing.spi.LicenseActivationProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Emergency activation: grants a short, time-boxed window when normal
 * validation is impossible (offline server, licence-server outage, network
 * failure mid-validation). It never grants an unlimited licence — the window
 * expires and the licence falls back to its configured expiry behaviour, so a
 * failed validation can never silently become permanent access.
 */
@Component
public class EmergencyActivationProvider implements LicenseActivationProvider {

    private static final int DEFAULT_GRANT_DAYS = 7;
    private static final int MAX_GRANT_DAYS = 30;

    @Override public String mode() { return "EMERGENCY"; }

    @Override public String label() { return "Emergency activation"; }

    @Override
    public ActivationDecision verify(String licenseKey, String fingerprint, Map<String, Object> context) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return ActivationDecision.deny("License key is required");
        }
        Object requested = context == null ? null : context.get("grantDays");
        int days = DEFAULT_GRANT_DAYS;
        if (requested instanceof Number n) {
            days = n.intValue();
        } else if (requested instanceof String s) {
            try {
                days = Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                days = DEFAULT_GRANT_DAYS;
            }
        }
        if (days <= 0 || days > MAX_GRANT_DAYS) {
            return ActivationDecision.deny("Emergency grant must be between 1 and " + MAX_GRANT_DAYS + " days");
        }
        return ActivationDecision.allow(days);
    }
}
