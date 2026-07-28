package com.sami.app.comm.spi;

/**
 * The outcome of one delivery attempt.
 *
 * <p>{@code retryable} is the handler's own judgement: a malformed recipient
 * will never succeed however often it is retried, while a timeout might. The
 * sweep honours it — a non-retryable failure goes terminal immediately instead
 * of burning the policy's remaining attempts.
 */
public record DeliveryResult(
        boolean success,
        boolean retryable,
        String providerMessageRef,
        String error
) {
    public static DeliveryResult ok(String providerMessageRef) {
        return new DeliveryResult(true, false, providerMessageRef, null);
    }

    public static DeliveryResult retryableFailure(String error) {
        return new DeliveryResult(false, true, null, error);
    }

    public static DeliveryResult permanentFailure(String error) {
        return new DeliveryResult(false, false, null, error);
    }
}
