package com.sami.app.auth;

import com.sami.app.auth.domain.RefreshToken;
import com.sami.app.auth.repository.RefreshTokenRepository;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.security.jwt.JwtService;
import com.sami.app.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Lifecycle of stored refresh tokens: issue, validate, rotate, revoke.
 *
 * <p>The client-facing token is still a refresh JWT (signature + expiry verified
 * by {@link JwtService}); this service adds the server-side state that makes those
 * tokens revocable — every issued token is stored as a SHA-256 hash and must be
 * present, unrevoked and unexpired to be accepted.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    /** Generates a refresh JWT for the user and stores its hash. */
    @Transactional
    public String issue(User user) {
        String token = jwtService.generateRefreshToken(user.getEmail());
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(token))
                .expiresAt(Instant.now().plusSeconds(jwtService.refreshTokenTtlSeconds()))
                .build();
        refreshTokenRepository.save(stored);
        return token;
    }

    /**
     * Resolves a raw token to its stored row, requiring it to be unrevoked and
     * unexpired. Reuse of a revoked (rotated-away) token fails here.
     *
     * @throws ApiException {@link ErrorCode#INVALID_TOKEN} if unknown, revoked or expired
     */
    @Transactional(readOnly = true)
    public RefreshToken validate(String rawToken) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
        if (!stored.isActive(Instant.now())) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
        return stored;
    }

    /** Rotation: revokes the presented token's row and issues a fresh one. */
    @Transactional
    public String rotate(RefreshToken current) {
        current.setRevokedAt(Instant.now());
        return issue(current.getUser());
    }

    /** Revokes the given raw token if it is known and still active (idempotent). */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .filter(stored -> stored.getRevokedAt() == null)
                .ifPresent(stored -> stored.setRevokedAt(Instant.now()));
    }

    /** Revokes every active token of a user ("log out everywhere"). */
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId, Instant.now());
    }
}
