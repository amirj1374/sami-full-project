package com.sami.app.portal.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.portal.PortalProperties;
import com.sami.app.portal.domain.PortalAccount;
import com.sami.app.portal.domain.PortalOtpChallenge;
import com.sami.app.portal.repository.PortalOtpChallengeRepository;
import com.sami.app.portal.spi.OtpDeliveryChannel;
import com.sami.app.portal.spi.OtpDeliveryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

/**
 * One-time codes.
 *
 * <p><b>Fails closed.</b> If no {@link OtpDeliveryChannel} bean is registered for
 * the requested method, this service REFUSES to issue a code rather than
 * generating one it cannot deliver. The alternative — falling back to the
 * existing mail port, which writes to the application log — would print customer
 * verification codes to stdout. That is a credential leak, not a degraded
 * experience, so it is refused explicitly.
 *
 * <p>Codes are stored hashed, never in plain text, exactly as refresh tokens are.
 */
@Service
@RequiredArgsConstructor
public class PortalOtpService {

    public static final String PURPOSE_VERIFY = "verify";
    public static final String PURPOSE_LOGIN = "login";

    private final PortalOtpChallengeRepository challengeRepository;
    private final OtpDeliveryRegistry deliveryRegistry;
    private final PortalProperties properties;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public PortalOtpChallenge issue(PortalAccount account, String purpose,
                                    String deliveryKey, String target) {
        OtpDeliveryChannel channel = deliveryRegistry.find(deliveryKey)
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        ("One-time codes are unavailable: no delivery channel is registered for "
                                + "'%s'. Register an OtpDeliveryChannel bean before enabling this "
                                + "authentication method.").formatted(deliveryKey)));

        if (target == null || target.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "No delivery address is on file for this account");
        }

        String code = generateCode();
        PortalOtpChallenge challenge = challengeRepository.save(PortalOtpChallenge.builder()
                .accountId(account.getId())
                .purpose(purpose)
                .codeHash(hash(code))
                .deliveryKey(deliveryKey)
                .deliveryTarget(target)
                .expiresAt(Instant.now().plus(properties.otpTtl()))
                .tenantId(account.getTenantId())
                .build());

        // The plaintext code exists only here and inside the channel.
        channel.deliver(target, code, account.getPreferredLanguage());
        return challenge;
    }

    /**
     * Consumes a code. Every failed attempt is counted, so a challenge cannot be
     * brute-forced within its lifetime.
     */
    @Transactional
    public boolean verify(Long accountId, String purpose, String code) {
        PortalOtpChallenge challenge = challengeRepository
                .findFirstByAccountIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(accountId, purpose)
                .orElse(null);

        if (challenge == null || !challenge.isUsable()) {
            return false;
        }
        challenge.setAttempts(challenge.getAttempts() + 1);

        boolean matches = MessageDigest.isEqual(
                challenge.getCodeHash().getBytes(), hash(code).getBytes());
        if (matches) {
            challenge.setConsumedAt(Instant.now());
        }
        challengeRepository.save(challenge);
        return matches;
    }

    /** True when at least one delivery channel exists — surfaced in the catalogue. */
    public boolean deliveryAvailable(String deliveryKey) {
        return deliveryRegistry.find(deliveryKey).isPresent();
    }

    private String generateCode() {
        int digits = properties.otpLength();
        StringBuilder code = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
