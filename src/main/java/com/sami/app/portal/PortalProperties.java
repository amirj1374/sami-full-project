package com.sami.app.portal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Portal configuration.
 *
 * <p>{@code jwtSecret} is deliberately SEPARATE from the staff JWT secret. Using
 * a different signing key means a staff token cannot verify as a portal token
 * and vice versa — cryptographic separation rather than a claim the code must
 * remember to check.
 *
 * @param maxFailedAttempts failures before an account locks
 * @param lockDuration      how long a lock lasts
 * @param otpLength         digits in a one-time code
 */
@ConfigurationProperties(prefix = "app.portal")
public record PortalProperties(boolean enabled,
                               String jwtSecret,
                               String issuer,
                               Duration accessTokenTtl,
                               Duration sessionTtl,
                               int maxFailedAttempts,
                               Duration lockDuration,
                               int otpLength,
                               Duration otpTtl) {

    public PortalProperties {
        issuer = issuer == null || issuer.isBlank() ? "sami-portal" : issuer;
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(30) : accessTokenTtl;
        sessionTtl = sessionTtl == null ? Duration.ofDays(30) : sessionTtl;
        maxFailedAttempts = maxFailedAttempts <= 0 ? 5 : maxFailedAttempts;
        lockDuration = lockDuration == null ? Duration.ofMinutes(15) : lockDuration;
        otpLength = otpLength <= 0 ? 6 : otpLength;
        otpTtl = otpTtl == null ? Duration.ofMinutes(5) : otpTtl;
    }
}
