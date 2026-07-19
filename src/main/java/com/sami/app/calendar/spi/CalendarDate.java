package com.sami.app.calendar.spi;

/**
 * A date expressed in some chronology's own numbering.
 *
 * <p>Deliberately not a {@code LocalDate}: 1405-04-29 is a perfectly valid
 * Solar Hijri date and a nonsensical Gregorian one, so the two must not share
 * a type. Conversion in either direction goes through {@link CalendarSystem}.
 */
public record CalendarDate(int year, int month, int day) {

    public CalendarDate {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month out of range: " + month);
        }
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException("day out of range: " + day);
        }
    }

    /** Zero-padded {@code yyyy-MM-dd}, the form used in exports and the UI. */
    public String iso() {
        return "%04d-%02d-%02d".formatted(year, month, day);
    }
}
