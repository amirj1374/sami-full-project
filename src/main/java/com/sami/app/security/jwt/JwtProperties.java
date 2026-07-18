package com.sami.app.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized JWT configuration, bound from {@code security.jwt.*}.
 *
 * @param secret            Base64-encoded HMAC secret (min 256 bits for HS256).
 *                          Supplied via environment variable in every environment.
 * @param issuer            token issuer claim
 * @param accessTokenTtl    access-token lifetime in seconds
 * @param refreshTokenTtl   refresh-token lifetime in seconds
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenTtl,
        long refreshTokenTtl
) {
}
