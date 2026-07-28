package com.sami.app.scheduling.api;

import java.time.Instant;
import java.util.List;

/**
 * Why a requested slot could not be taken.
 *
 * <p>{@code reason} is a stable enum-like code so callers and the UI can branch
 * without parsing prose.
 */
public record BookingConflict(
        Reason reason,
        Long resourceId,
        String detail,
        Instant conflictStart,
        Instant conflictEnd,
        List<Long> conflictingScheduleIds
) {
    public enum Reason {
        /** Another live reservation overlaps on the same resource. */
        RESOURCE_DOUBLE_BOOKED,
        /** The resource's status forbids new bookings (maintenance, retired). */
        RESOURCE_UNAVAILABLE,
        /** The date is a holiday or closure, or appointments are blocked. */
        OUTSIDE_BUSINESS_DAY,
        /** The window falls outside every bookable shift, or spans two. */
        OUTSIDE_WORKING_HOURS,
        /** No resource matched the required skills. */
        NO_SUITABLE_RESOURCE,
        /** Booking is too soon, or too far ahead, for the appointment type. */
        NOTICE_PERIOD_VIOLATED,
        /** Requested duration is outside the type's permitted range. */
        DURATION_NOT_ALLOWED
    }

    public static BookingConflict of(Reason reason, Long resourceId, String detail) {
        return new BookingConflict(reason, resourceId, detail, null, null, List.of());
    }
}
