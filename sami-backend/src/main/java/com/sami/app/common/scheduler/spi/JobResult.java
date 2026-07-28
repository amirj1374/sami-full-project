package com.sami.app.common.scheduler.spi;

/**
 * The outcome of one run.
 *
 * <p>{@code itemsProcessed} is recorded so a sweep that legitimately found
 * nothing is distinguishable from one that failed silently — the single most
 * useful field when diagnosing "did the job actually run?".
 */
public record JobResult(boolean success, String outcome, Integer itemsProcessed) {

    public static JobResult ok(String outcome) {
        return new JobResult(true, outcome, null);
    }

    public static JobResult ok(String outcome, int itemsProcessed) {
        return new JobResult(true, outcome, itemsProcessed);
    }

    public static JobResult failed(String reason) {
        return new JobResult(false, reason, null);
    }
}
