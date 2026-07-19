package com.sami.app.calendar.spi;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Gregorian calendar — the identity conversion, since storage is already
 * Gregorian.
 *
 * <p>It exists so that no caller needs a special case for "the system that
 * needs no conversion": every path goes through the same SPI.
 */
@Component
public class GregorianCalendarSystem implements CalendarSystem {

    public static final String KEY = "gregorian";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Gregorian";
    }

    @Override
    public CalendarDate fromGregorian(LocalDate date) {
        return new CalendarDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    @Override
    public LocalDate toGregorian(CalendarDate date) {
        return LocalDate.of(date.year(), date.month(), date.day());
    }

    @Override
    public int lengthOfMonth(int year, int month) {
        return Month.of(month).length(java.time.Year.isLeap(year));
    }

    @Override
    public boolean isLeapYear(int year) {
        return java.time.Year.isLeap(year);
    }

    @Override
    public String monthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    /**
     * Overridden because the generic two-year search in the interface default
     * is unnecessary here — a Gregorian recurrence lands in its own year by
     * definition. 29 February in a common year has no occurrence.
     */
    @Override
    public LocalDate resolveRecurrence(int gregorianYear, int month, int day) {
        if (day > lengthOfMonth(gregorianYear, month)) {
            return null;
        }
        return LocalDate.of(gregorianYear, month, day);
    }
}
