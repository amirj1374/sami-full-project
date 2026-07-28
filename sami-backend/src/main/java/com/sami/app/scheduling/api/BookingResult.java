package com.sami.app.scheduling.api;

import java.util.List;
import java.util.Optional;

/**
 * The outcome of a booking attempt.
 *
 * <p>A failed attempt is a value, not an exception, because "that slot is
 * taken, here are three others" is an ordinary business answer rather than an
 * error. Genuine faults (unknown appointment type, missing calendar) still
 * throw.
 */
public record BookingResult(
        Long scheduleId,
        String scheduleNumber,
        boolean booked,
        boolean awaitingApproval,
        BookingConflict conflict,
        List<TimeSlot> alternatives,
        Long waitingListEntryId,
        String message
) {
    public boolean failed() {
        return !booked && !awaitingApproval;
    }

    public Optional<BookingConflict> conflictDetail() {
        return Optional.ofNullable(conflict);
    }
}
