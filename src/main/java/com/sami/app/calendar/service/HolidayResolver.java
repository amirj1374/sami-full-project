package com.sami.app.calendar.service;

import com.sami.app.calendar.domain.Holiday;
import com.sami.app.calendar.spi.CalendarSystem;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Decides whether a given date falls on a holiday.
 *
 * <p>Deliberately free of repositories: it takes the candidate rows and the
 * chronology it needs, so the matching rules can be tested exhaustively without
 * a database. The awkward part it exists to contain is that a recurring holiday
 * has no stored Gregorian date — 1 Farvardin must be resolved per year through
 * the calendar SPI, which SQL cannot do.
 */
@Component
public class HolidayResolver {

    /**
     * The first holiday covering {@code date}, if any.
     *
     * @param systemLookup resolves a {@code calendar_systems} id to its SPI
     *                     implementation; called only for recurring rows
     */
    public Optional<Holiday> match(LocalDate date, List<Holiday> candidates,
                                   Function<Long, CalendarSystem> systemLookup) {
        return candidates.stream()
                .filter(Holiday::isActive)
                .filter(h -> covers(h, date, systemLookup))
                .findFirst();
    }

    /** True when {@code date} lies within the holiday's observance span. */
    public boolean covers(Holiday holiday, LocalDate date,
                          Function<Long, CalendarSystem> systemLookup) {
        if (!holiday.isRecurring()) {
            LocalDate start = holiday.getHolidayDate();
            return start != null && withinSpan(date, start, holiday.getDurationDays());
        }

        CalendarSystem system = systemLookup.apply(holiday.getRecurrenceSystemId());
        if (system == null) {
            // Configuration names a chronology with no implementation. Treating
            // the day as a working day is the safe failure: it may let a
            // booking through, whereas guessing a date could close the shop on
            // an arbitrary day. The registry logs the missing handler.
            return false;
        }

        // A span can start in the previous Gregorian year and run into this
        // one (a late-Esfand observance crossing Nowruz), so neighbouring
        // years are checked too rather than assuming alignment.
        for (int year = date.getYear() - 1; year <= date.getYear() + 1; year++) {
            LocalDate start = system.resolveRecurrence(
                    year, holiday.getRecurrenceMonth(), holiday.getRecurrenceDay());
            if (start != null && withinSpan(date, start, holiday.getDurationDays())) {
                return true;
            }
        }
        return false;
    }

    private boolean withinSpan(LocalDate date, LocalDate start, int durationDays) {
        int days = Math.max(1, durationDays);
        return !date.isBefore(start) && date.isBefore(start.plusDays(days));
    }
}
