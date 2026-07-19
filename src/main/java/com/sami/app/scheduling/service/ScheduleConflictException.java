package com.sami.app.scheduling.service;

import com.sami.app.scheduling.api.BookingConflict;
import lombok.Getter;

/**
 * Raised when a booking cannot proceed.
 *
 * <p>Carries the structured {@link BookingConflict} rather than only a message
 * so the caller can branch on the reason and, for an overlap, offer
 * alternatives without re-deriving what went wrong.
 *
 * <p>This is thrown from inside the booking transaction — including when it is
 * the DATABASE that rejected the write — so the transaction rolls back and no
 * half-created appointment survives.
 */
@Getter
public class ScheduleConflictException extends RuntimeException {

    private final transient BookingConflict conflict;

    public ScheduleConflictException(BookingConflict conflict) {
        super(conflict.detail());
        this.conflict = conflict;
    }
}
