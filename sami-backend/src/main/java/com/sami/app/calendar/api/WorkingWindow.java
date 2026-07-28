package com.sami.app.calendar.api;

import java.time.LocalTime;

/**
 * A bookable span within a single day, in the calendar's local time.
 *
 * <p>Part of the module's public contract: the scheduler consumes these and
 * never reads shift rows directly.
 */
public record WorkingWindow(
        String shiftCode,
        String shiftName,
        LocalTime start,
        LocalTime end,
        int maxConcurrent,
        boolean overtime
) {

    public boolean contains(LocalTime time) {
        return !time.isBefore(start) && time.isBefore(end);
    }

    /** True when {@code [from, to)} lies wholly inside this window. */
    public boolean encloses(LocalTime from, LocalTime to) {
        return !from.isBefore(start) && !to.isAfter(end);
    }

    public int minutes() {
        return (int) java.time.Duration.between(start, end).toMinutes();
    }
}
