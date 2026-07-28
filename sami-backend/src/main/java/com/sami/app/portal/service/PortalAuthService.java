package com.sami.app.portal.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.portal.PortalProperties;
import com.sami.app.portal.domain.PortalAccount;
import com.sami.app.portal.domain.PortalSession;
import com.sami.app.portal.event.PortalDomainEvent;
import com.sami.app.portal.repository.PortalAccountRepository;
import com.sami.app.portal.repository.PortalAccountStatusRepository;
import com.sami.app.portal.repository.PortalSessionRepository;
import com.sami.app.portal.security.PortalTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Portal authentication.
 *
 * <p>Account lockout lives here because this is the first login surface exposed
 * to the open internet and the system has no shared rate limiter. Failures are
 * counted per account and a lock is time-boxed, so a password-guessing run is
 * bounded without permanently denying the real customer access.
 *
 * <p>Login responses are deliberately uniform: an unknown username and a wrong
 * password produce the same error, so the endpoint cannot be used to enumerate
 * which customers have portal accounts.
 */
@Service
@RequiredArgsConstructor
public class PortalAuthService {

    private final PortalAccountRepository accountRepository;
    private final PortalAccountStatusRepository statusRepository;
    private final PortalSessionRepository sessionRepository;
    private final PortalTokenService tokenService;
    private final PortalAuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final PortalProperties properties;
    private final ApplicationEventPublisher events;

    public record LoginResult(String accessToken, String sessionToken, long expiresInSeconds,
                              boolean mustChangePassword, Long accountId) {
    }

    @Transactional
    public LoginResult login(String username, String password, String deviceLabel,
                             String deviceFingerprint, String ip, String userAgent) {
        PortalAccount account = accountRepository.findByUsername(username)
                .or(() -> accountRepository.findByMobileNumber(username))
                .orElse(null);

        // Uniform failure: never reveal whether the account exists.
        if (account == null) {
            throw invalidCredentials();
        }
        if (account.isTemporarilyLocked()) {
            throw new ApiException(ErrorCode.ACCESS_DENIED,
                    "This account is temporarily locked. Try again later.");
        }
        if (!account.getStatus().isAllowsLogin()) {
            if (account.getStatus().isRequiresVerification()) {
                throw new ApiException(ErrorCode.ACCESS_DENIED,
                        "This account has not been verified yet");
            }
            throw new ApiException(ErrorCode.ACCESS_DENIED, "This account cannot sign in");
        }
        if (account.getPasswordHash() == null
                || !passwordEncoder.matches(password, account.getPasswordHash())) {
            registerFailure(account);
            throw invalidCredentials();
        }

        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        account.setLastLoginAt(Instant.now());
        account.setLastLoginIp(ip);
        accountRepository.save(account);

        PortalSession session = sessionRepository.save(PortalSession.builder()
                .accountId(account.getId())
                .tokenHash(sha256(tokenService.newSessionToken()))
                .deviceLabel(deviceLabel)
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ip)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plus(properties.sessionTtl()))
                .tenantId(account.getTenantId())
                .build());

        auditService.record(account.getId(), "account", account.getId(),
                PortalAuditService.LOGIN, null, Map.of("device", deviceLabel == null ? "" : deviceLabel));
        events.publishEvent(PortalDomainEvent.of(PortalDomainEvent.CUSTOMER_LOGGED_IN,
                account.getId(), account.getCustomerId(), Map.of()));

        return new LoginResult(
                tokenService.issue(account.getId(), session.getId()),
                null,
                properties.accessTokenTtl().toSeconds(),
                account.isMustChangePassword(),
                account.getId());
    }

    /**
     * Counts a failure and locks the account once the threshold is reached.
     * Locking also revokes live sessions, so an attacker who already holds a
     * token loses it at the same moment.
     */
    private void registerFailure(PortalAccount account) {
        account.setFailedAttempts(account.getFailedAttempts() + 1);
        if (account.getFailedAttempts() >= properties.maxFailedAttempts()) {
            account.setLockedUntil(Instant.now().plus(properties.lockDuration()));
            sessionRepository.revokeAllForAccount(account.getId(), Instant.now(),
                    "Account locked after repeated failed sign-in attempts");
            auditService.record(account.getId(), "account", account.getId(),
                    PortalAuditService.LOCKED, null,
                    Map.of("failedAttempts", account.getFailedAttempts()));
        } else {
            auditService.record(account.getId(), "account", account.getId(),
                    PortalAuditService.LOGIN_FAILED, null,
                    Map.of("failedAttempts", account.getFailedAttempts()));
        }
        accountRepository.save(account);
    }

    @Transactional
    public void logout(Long accountId, Long sessionId) {
        sessionRepository.findById(sessionId)
                .filter(s -> s.getAccountId().equals(accountId))
                .ifPresent(s -> {
                    s.setRevokedAt(Instant.now());
                    s.setRevokedReason("Signed out");
                    sessionRepository.save(s);
                });
        auditService.record(accountId, "session", sessionId, PortalAuditService.LOGOUT, null, null);
    }

    @Transactional(readOnly = true)
    public List<PortalSession> sessions(Long accountId) {
        return sessionRepository.findAllByAccountIdAndRevokedAtIsNullOrderByIssuedAtDesc(accountId);
    }

    /** Revokes one device's session. A customer may only revoke their own. */
    @Transactional
    public void revokeSession(Long accountId, Long sessionId) {
        PortalSession session = sessionRepository.findById(sessionId)
                .filter(s -> s.getAccountId().equals(accountId))
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Unknown session"));
        session.setRevokedAt(Instant.now());
        session.setRevokedReason("Revoked by customer");
        sessionRepository.save(session);
        auditService.record(accountId, "session", sessionId,
                PortalAuditService.SESSION_REVOKED, null, null);
    }

    @Transactional
    public void changePassword(Long accountId, String currentPassword, String newPassword) {
        PortalAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Unknown account"));
        if (account.getPasswordHash() != null
                && !passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH, "Current password is incorrect");
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setMustChangePassword(false);
        accountRepository.save(account);

        // Every other device is signed out, so a stolen session dies with the password.
        sessionRepository.revokeAllForAccount(accountId, Instant.now(), "Password changed");
        auditService.record(accountId, "account", accountId, "PasswordChanged", null, null);
    }

    private ApiException invalidCredentials() {
        return new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
