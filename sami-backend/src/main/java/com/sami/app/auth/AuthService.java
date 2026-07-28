package com.sami.app.auth;

import com.sami.app.auth.domain.PasswordResetToken;
import com.sami.app.auth.domain.RefreshToken;
import com.sami.app.auth.dto.AuthResponse;
import com.sami.app.auth.dto.ChangePasswordRequest;
import com.sami.app.auth.dto.ForgotPasswordRequest;
import com.sami.app.auth.dto.LoginRequest;
import com.sami.app.auth.dto.LogoutRequest;
import com.sami.app.auth.dto.RefreshRequest;
import com.sami.app.auth.dto.RegisterRequest;
import com.sami.app.auth.dto.ResetPasswordRequest;
import com.sami.app.auth.repository.PasswordResetTokenRepository;
import com.sami.app.authz.domain.Role;
import com.sami.app.authz.repository.RoleRepository;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.mail.MailSender;
import com.sami.app.security.jwt.JwtService;
import com.sami.app.user.domain.User;
import com.sami.app.user.domain.UserProfile;
import com.sami.app.user.domain.UserStatus;
import com.sami.app.user.dto.CurrentUserResponse;
import com.sami.app.user.repository.UserProfileRepository;
import com.sami.app.user.repository.UserRepository;
import com.sami.app.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Authentication use-cases: registration, login, token refresh/rotation, logout
 * and the password lifecycle (change, forgot, reset).
 *
 * <p>Keeping this logic in a service (not the controller) means the same flows can
 * be reused by other entry points later (e.g. an admin bootstrap task or a future
 * OAuth bridge) without duplication. Refresh tokens are stored hashed server-side
 * (see {@link RefreshTokenService}), so most flows here open write transactions.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Password-reset tokens are single-use and expire after one hour. */
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(60);

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserStatusRepository statusRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final MailSender mailSender;

    /** Registers a new user with the database-configured default role. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Which role and status newcomers get is data, not code: exactly one role
        // and one status carry the is_default flag (DB-enforced). Their absence
        // is a deployment error.
        Role defaultRole = roleRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default role is configured"));
        UserStatus defaultStatus = statusRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default user status is configured"));

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(defaultRole)
                .status(defaultStatus)
                .build();
        userRepository.save(user);

        // Every user owns a profile from day one; self-registration starts it
        // with the display name only.
        profileRepository.save(UserProfile.builder()
                .user(user)
                .displayName(request.fullName())
                .build());

        return issueTokens(user);
    }

    /** Verifies credentials and returns a fresh token pair. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Delegates password verification to Spring Security (BadCredentials -> 401).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        return issueTokens(user);
    }

    /**
     * Exchanges a valid refresh token for a new access/refresh pair (rotation).
     *
     * <p>The token must both verify as a refresh JWT and exist server-side by hash,
     * unrevoked and unexpired. The presented token's row is revoked on success, so
     * replaying it later fails with {@link ErrorCode#INVALID_TOKEN}.
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        final String subject;
        try {
            if (!jwtService.isRefreshToken(request.refreshToken())) {
                throw new ApiException(ErrorCode.INVALID_TOKEN);
            }
            subject = jwtService.extractUsername(request.refreshToken());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }

        RefreshToken stored = refreshTokenService.validate(request.refreshToken());

        User user = userRepository.findByEmail(subject)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
        if (!user.isLoginAllowed()) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }

        return buildResponse(user, refreshTokenService.rotate(stored));
    }

    /** Revokes the presented refresh token. Idempotent; unknown tokens are ignored. */
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    /**
     * Changes the authenticated user's password after verifying the current one,
     * then revokes all refresh tokens so stolen sessions die with the old password.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllForUser(user.getId());
    }

    /**
     * Starts the password-reset flow. Deliberately succeeds whether or not the
     * email exists so the endpoint cannot be used to enumerate accounts; only known
     * addresses get a token.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = TokenHasher.generateToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(TokenHasher.sha256Hex(token))
                    .expiresAt(Instant.now().plus(RESET_TOKEN_TTL))
                    .build();
            passwordResetTokenRepository.save(resetToken);
            // The plaintext token only ever travels via the mail port.
            mailSender.sendPasswordReset(user.getEmail(), token);
        });
    }

    /**
     * Completes the reset flow: validates the token (exists, unused, unexpired),
     * marks it used, stores the new password and revokes all refresh tokens.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(TokenHasher.sha256Hex(request.token()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
        if (!resetToken.isUsable(Instant.now())) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }

        resetToken.setUsedAt(Instant.now());
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllForUser(user.getId());
    }

    private AuthResponse issueTokens(User user) {
        return buildResponse(user, refreshTokenService.issue(user));
    }

    private AuthResponse buildResponse(User user, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        return AuthResponse.of(accessToken, refreshToken, jwtService.accessTokenTtlSeconds(),
                CurrentUserResponse.from(user));
    }
}
