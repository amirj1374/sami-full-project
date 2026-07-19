package com.sami.app.calendar.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The Business Calendar module's public service.
 *
 * <p>This is the seam the master scheduler and every other module consume: no
 * caller reads {@code calendar_shifts} or {@code holidays} directly, and no
 * module reimplements "is tomorrow a working day". Publishing it as an
 * interface also means a deployment can substitute an implementation backed by
 * an external corporate calendar without touching a single caller.
 */
public interface WorkingTimeProvider {

    /** Resolves the calendar that governs a branch, falling back company → tenant default. */
    Long resolveCalendarId(Long companyId, Long branchId);

    /** The fully-resolved picture of one date: template, holidays and exceptions applied. */
    DaySchedule scheduleFor(Long calendarId, LocalDate date);

    List<DaySchedule> scheduleForRange(Long calendarId, LocalDate from, LocalDate to);

    boolean isWorkingDay(Long calendarId, LocalDate date);

    /** True when appointments may be placed on this date at all. */
    boolean isBookable(Long calendarId, LocalDate date);

    /**
     * True when {@code [start, end)} lies wholly inside a single bookable
     * window. An appointment spanning a lunch closure is rejected, which is
     * the "appointment exceeds working hours" edge case.
     */
    boolean isWithinWorkingHours(Long calendarId, Instant start, Instant end);

    /** The next working day strictly after {@code from}. */
    LocalDate nextWorkingDay(Long calendarId, LocalDate from);

    /**
     * Adds business days, skipping non-working ones.
     *
     * @param days may be negative to count backwards
     */
    LocalDate addBusinessDays(Long calendarId, LocalDate from, int days);

    /** Count of working days in {@code [from, to]}, inclusive. */
    int countBusinessDays(Long calendarId, LocalDate from, LocalDate to);
}
