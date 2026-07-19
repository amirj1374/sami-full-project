package com.sami.app.licensing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Licensing behaviour, bound from {@code app.licensing.*}.
 *
 * @param enforce      when false (the default) an installation with no licence
 *                     configured runs unrestricted — existing/on-premise
 *                     deployments keep working until licensing is rolled out.
 *                     Turn on to require a valid licence for gated features.
 * @param cacheSeconds how long a resolved entitlement snapshot is reused, so
 *                     feature-flag changes take effect without a restart while
 *                     avoiding a database hit per check.
 */
@ConfigurationProperties(prefix = "app.licensing")
public record LicensingProperties(boolean enforce, int cacheSeconds) {

    public int cacheSecondsOrDefault() {
        return cacheSeconds > 0 ? cacheSeconds : 30;
    }
}
