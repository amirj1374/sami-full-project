package com.sami.app.portal.security;

import com.sami.app.portal.PortalProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Issues and verifies portal access tokens.
 *
 * <p>Signed with a key that is separate from the staff JWT key. That separation
 * is cryptographic, not conventional: a staff token cannot verify here and a
 * portal token cannot verify in {@code JwtService}, regardless of what any claim
 * says or which filter runs.
 *
 * <p>Like the staff tokens, capabilities are NOT embedded — they are reloaded
 * from the database on every request, so revoking a capability or locking an
 * account takes effect immediately rather than at token expiry.
 */
@Service
public class PortalTokenService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_SESSION = "sid";
    private static final String TYPE_PORTAL = "portal-access";

    private final PortalProperties properties;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public PortalTokenService(PortalProperties properties) {
        this.properties = properties;
        String secret = properties.jwtSecret();
        if (secret == null || secret.length() < 32) {
            // Refuse a weak or absent key rather than silently signing with one.
            throw new IllegalStateException(
                    "app.portal.jwt-secret must be set and at least 32 characters, "
                            + "and must differ from the staff JWT secret");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String issue(Long accountId, Long sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(accountId))
                .claims(Map.of(CLAIM_TYPE, TYPE_PORTAL, CLAIM_SESSION, sessionId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    /** @return the claims of a valid portal token; throws on anything else. */
    public Claims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!TYPE_PORTAL.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new IllegalArgumentException("Not a portal token");
        }
        return claims;
    }

    public Long accountId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public Long sessionId(Claims claims) {
        Object value = claims.get(CLAIM_SESSION);
        return value instanceof Number n ? n.longValue() : null;
    }

    /** Opaque refresh/session token. Stored hashed, like the staff refresh tokens. */
    public String newSessionToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
