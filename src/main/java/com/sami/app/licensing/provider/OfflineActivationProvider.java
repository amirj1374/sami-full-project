package com.sami.app.licensing.provider;

import com.sami.app.licensing.spi.LicenseActivationProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Offline activation: the key is verified against its own structure, with no
 * network call — the mode an air-gapped on-premise server uses. A stronger
 * scheme (signed payload / public-key verification) drops in behind this same
 * provider without touching callers.
 */
@Component
public class OfflineActivationProvider implements LicenseActivationProvider {

    @Override public String mode() { return "OFFLINE"; }

    @Override public String label() { return "Offline activation"; }

    @Override
    public ActivationDecision verify(String licenseKey, String fingerprint, Map<String, Object> context) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return ActivationDecision.deny("License key is required");
        }
        // Structural check of the issued key format (SAMI-XXXXX-XXXXX-XXXXX-XXXXX).
        if (!licenseKey.matches("^SAMI(-[A-Z0-9]{5}){4}$")) {
            return ActivationDecision.deny("License key format is not recognised");
        }
        return ActivationDecision.allow();
    }
}
