package com.sami.app.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token utilities: opaque random tokens and one-way hashing.
 *
 * <p>Raw tokens are handed to clients; only their SHA-256 hex digest is persisted
 * (see {@code refresh_tokens} / {@code password_reset_tokens}), so a database leak
 * never exposes usable tokens. SHA-256 (not BCrypt) is fine here because the
 * inputs are high-entropy random values, not passwords.
 */
public final class TokenHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private TokenHasher() {
    }

    /** @return a 32-byte cryptographically random token, URL-safe Base64 encoded. */
    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** @return the lowercase SHA-256 hex digest (64 chars) of the given value. */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // Every JVM is required to ship SHA-256; this is unreachable in practice.
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
