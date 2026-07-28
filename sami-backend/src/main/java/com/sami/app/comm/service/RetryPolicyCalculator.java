package com.sami.app.comm.service;

import com.sami.app.comm.domain.CommDeliveryPolicy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Pure arithmetic over a delivery policy: when is the next attempt, and is
 * there one at all.
 *
 * <p>Backoff is {@code base * multiplier^(attempt-1)}, capped at the policy's
 * maximum — so STANDARD (60s, ×2) yields 60s, 120s, and OTP (10s, ×1) yields a
 * flat 10s. Kept free of repositories and clocks-as-side-effects so every
 * branch is unit-testable.
 */
@Component
public class RetryPolicyCalculator {

    /**
     * When the next attempt should run, or empty when the policy is exhausted.
     *
     * @param attemptsMade attempts already made, including the one that just failed
     */
    public Optional<Instant> nextAttemptAt(CommDeliveryPolicy policy, int attemptsMade, Instant now) {
        if (attemptsMade >= policy.getMaxAttempts()) {
            return Optional.empty();
        }
        double backoff = policy.getRetryBaseSeconds()
                * Math.pow(policy.getRetryBackoffMultiplier().doubleValue(), Math.max(0, attemptsMade - 1));
        long seconds = Math.min((long) Math.ceil(backoff), policy.getRetryMaxSeconds());
        return Optional.of(now.plus(Duration.ofSeconds(Math.max(1, seconds))));
    }

    /** The instant a message queued now stops being worth delivering. 0 = never. */
    public Optional<Instant> expiresAt(CommDeliveryPolicy policy, Instant queuedAt) {
        if (policy.getExpirationMinutes() <= 0) {
            return Optional.empty();
        }
        return Optional.of(queuedAt.plus(Duration.ofMinutes(policy.getExpirationMinutes())));
    }

    /** True when the message's useful life ended before it could be delivered. */
    public boolean isExpired(Instant expiresAt, Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }
}
