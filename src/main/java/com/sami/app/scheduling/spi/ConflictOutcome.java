package com.sami.app.scheduling.spi;

import com.sami.app.scheduling.api.TimeSlot;

import java.util.List;

/**
 * What a {@link ConflictResolutionStrategy} decided.
 *
 * @param accepted        book anyway (only legal when the policy is non-blocking)
 * @param alternatives    suggested free slots, possibly empty
 * @param requiresApproval hold the booking pending a manager decision
 * @param message         human-readable explanation surfaced to the caller
 */
public record ConflictOutcome(
        boolean accepted,
        List<TimeSlot> alternatives,
        boolean requiresApproval,
        String message
) {
    public static ConflictOutcome reject(String message) {
        return new ConflictOutcome(false, List.of(), false, message);
    }

    public static ConflictOutcome suggest(List<TimeSlot> alternatives, String message) {
        return new ConflictOutcome(false, List.copyOf(alternatives), false, message);
    }

    public static ConflictOutcome allow(String message) {
        return new ConflictOutcome(true, List.of(), false, message);
    }

    public static ConflictOutcome needsApproval(String message) {
        return new ConflictOutcome(false, List.of(), true, message);
    }
}
