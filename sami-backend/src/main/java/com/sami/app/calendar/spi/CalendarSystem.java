package com.sami.app.calendar.spi;

import java.time.LocalDate;

/**
 * Converts between a chronology and the Gregorian {@link LocalDate} that every
 * table stores.
 *
 * <p>Implementations are resolved by {@code calendar_systems.handler_key}, so a
 * new chronology is a configuration row plus a bean — no migration, no change
 * to any caller. This is the extension point the module exists to provide.
 *
 * <p>Implementations must be stateless and thread-safe.
 */
public interface CalendarSystem {

    /** Matches {@code calendar_systems.handler_key}. */
    String key();

    /** Human-readable name for logs and error messages. */
    String displayName();

    /** Gregorian → this chronology. */
    CalendarDate fromGregorian(LocalDate date);

    /** This chronology → Gregorian. */
    LocalDate toGregorian(CalendarDate date);

    /** Days in the given month, which varies by year in most chronologies. */
    int lengthOfMonth(int year, int month);

    boolean isLeapYear(int year);

    /** Month name in this chronology, 1-based. Used by exports and pickers. */
    String monthName(int month);

    /**
     * Resolves an annually recurring month/day to its Gregorian date in the
     * given Gregorian year.
     *
     * <p>This is what makes recurring holidays correct: 1 Farvardin is a fixed
     * point in the Persian year but moves against the Gregorian one, so the
     * answer must be computed per year rather than stored.
     *
     * @return the Gregorian date, or {@code null} when the recurrence does not
     *         occur in that Gregorian year at all — which is possible near the
     *         year boundary and must not be silently coerced to a wrong date
     */
    default LocalDate resolveRecurrence(int gregorianYear, int month, int day) {
        // A recurrence in month/day of THIS chronology can fall in one of two
        // adjacent chronology-years when projected onto a Gregorian year.
        int candidateYear = fromGregorian(LocalDate.of(gregorianYear, 1, 1)).year();
        for (int y = candidateYear; y <= candidateYear + 1; y++) {
            if (day > lengthOfMonth(y, month)) {
                continue;
            }
            LocalDate g = toGregorian(new CalendarDate(y, month, day));
            if (g.getYear() == gregorianYear) {
                return g;
            }
        }
        return null;
    }
}
