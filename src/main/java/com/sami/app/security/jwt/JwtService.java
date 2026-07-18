package com.sami.app.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Creates and validates signed JWTs (HS256).
 *
 * <p>Tokens carry identity only (subject + a {@code type} claim distinguishing
 * access from refresh tokens). Authorities are deliberately NOT embedded: they are
 * loaded from the database on every request, so permission and role changes take
 * effect immediately instead of at token expiry.
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes());
    }

    public String generateAccessToken(String subject) {
        return buildToken(subject, Map.of(CLAIM_TYPE, TYPE_ACCESS), properties.accessTokenTtl());
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, Map.of(CLAIM_TYPE, TYPE_REFRESH), properties.refreshTokenTtl());
    }

    /** @return the subject (email) of a valid, unexpired token. */
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    /** @return true if the token verifies and its {@code type} claim is {@code access}. */
    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parse(token).get(CLAIM_TYPE, String.class));
    }

    /** @return true if the token verifies and its {@code type} claim is {@code refresh}. */
    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parse(token).get(CLAIM_TYPE, String.class));
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl();
    }

    public long refreshTokenTtlSeconds() {
        return properties.refreshTokenTtl();
    }

    /**
     * Parses and verifies signature + expiration. Throws a jjwt exception on any
     * problem, which the caller/global handler treats as an invalid token.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(String subject, Map<String, ?> claims, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }
}
