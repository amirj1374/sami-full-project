package com.sami.app.scheduling.api;

import java.time.Instant;

/**
 * A candidate booking window on a specific resource.
 *
 * <p>{@code start}/{@code end} are the CUSTOMER-facing window. The resource is
 * held longer when the appointment type declares preparation, cleanup or
 * buffer minutes — {@code resourceStart}/{@code resourceEnd} carry that wider
 * span, and it is the wider one the overlap check uses.
 */
public record TimeSlot(
        Long resourceId,
        String resourceName,
        Instant start,
        Instant end,
        Instant resourceStart,
        Instant resourceEnd
) {
    public static TimeSlot of(Long resourceId, String resourceName,
                              Instant start, Instant end,
                              Instant resourceStart, Instant resourceEnd) {
        return new TimeSlot(resourceId, resourceName, start, end, resourceStart, resourceEnd);
    }
}
