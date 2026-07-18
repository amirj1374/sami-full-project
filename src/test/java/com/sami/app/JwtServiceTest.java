package com.sami.app;

import com.sami.app.security.jwt.JwtProperties;
import com.sami.app.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}. Pure (no Spring context, no database) so they
 * run fast and verify the token contract in isolation.
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "test-secret-that-is-at-least-256-bits-long-000000",
                "sami-test",
                900,
                1209600);
        jwtService = new JwtService(props);
    }

    @Test
    void accessTokenRoundTripsSubjectAndIsTypedAccess() {
        String token = jwtService.generateAccessToken("user@example.com");

        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    void refreshTokenIsTypedRefreshAndNeverAccess() {
        String token = jwtService.generateRefreshToken("user@example.com");

        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken("user@example.com");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(Exception.class);
    }
}
