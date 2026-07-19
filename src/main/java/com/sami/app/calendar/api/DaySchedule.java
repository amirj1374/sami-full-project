package com.sami.app.calendar.api;

import java.time.LocalDate;
import java.util.List;

/**
 * What a single date looks like once the weekly template, holidays and
 * exceptions have all been applied.
 *
 * <p>{@code workingDay} and {@code appointmentsBlocked} are independent: a
 * stocktake day is worked but takes no bookings. Callers that conflate them
 * will let customers book into a closed shop, so both are surfaced explicitly
 * rather than collapsed into one boolean.
 */
public record DaySchedule(
        LocalDate date,
        boolean workingDay,
        boolean appointmentsBlocked,
        List<WorkingWindow> windows,
        String reason
) {

    public static DaySchedule closed(LocalDate date, String reason) {
        return new DaySchedule(date, false, true, List.of(), reason);
    }

    public static DaySchedule open(LocalDate date, List<WorkingWindow> windows) {
        return new DaySchedule(date, true, false, windows, null);
    }

    /** True when an appointment may actually be placed on this date. */
    public boolean bookable() {
        return workingDay && !appointmentsBlocked && !windows.isEmpty();
    }

    public int workingMinutes() {
        return windows.stream().mapToInt(WorkingWindow::minutes).sum();
    }
}
