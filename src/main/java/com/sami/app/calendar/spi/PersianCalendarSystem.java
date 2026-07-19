package com.sami.app.calendar.spi;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Solar Hijri (Jalali / Persian) calendar.
 *
 * <p><b>Why this is hand-implemented.</b> {@code java.time.chrono} ships
 * {@code HijrahChronology}, which is the Islamic LUNAR calendar: its year is
 * ~354 days and drifts about eleven days annually against the solar one. Using
 * it for Iranian business dates would put Nowruz on a different day every year
 * and silently corrupt every holiday calculation. There is no Solar Hijri
 * chronology in the JDK, and the project has no third-party date library, so
 * the arithmetic lives here.
 *
 * <p><b>Algorithm.</b> The Birashk/Borkowski leap-year breaks table, the same
 * one used by the reference {@code jalaali-js} implementation. It is exact for
 * Jalali years -61..3177, which spans every date this system will ever hold.
 * A naive 33-year-cycle approximation is NOT used: it disagrees with the
 * official Iranian calendar in several years and would put Nowruz a day out.
 *
 * <p>All arithmetic is integer and truncating, matching the reference
 * implementation's {@code ~~(a/b)} semantics — Java's {@code /} and {@code %}
 * truncate toward zero identically, so the port is direct.
 */
@Component
public class PersianCalendarSystem implements CalendarSystem {

    public static final String KEY = "persian";

    /** Jalali years at which the leap-year pattern shifts. */
    private static final int[] BREAKS = {
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
            1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    };

    private static final String[] MONTHS = {
            "Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
            "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"
    };

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Solar Hijri (Jalali)";
    }

    @Override
    public CalendarDate fromGregorian(LocalDate date) {
        return julianDayToJalali(gregorianToJulianDay(
                date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
    }

    @Override
    public LocalDate toGregorian(CalendarDate date) {
        if (date.day() > lengthOfMonth(date.year(), date.month())) {
            throw new IllegalArgumentException(
                    "Jalali date %s does not exist: %s has %d days in %d"
                            .formatted(date.iso(), monthName(date.month()),
                                    lengthOfMonth(date.year(), date.month()), date.year()));
        }
        int[] g = julianDayToGregorian(jalaliToJulianDay(date.year(), date.month(), date.day()));
        return LocalDate.of(g[0], g[1], g[2]);
    }

    /** Months 1–6 have 31 days, 7–11 have 30, and Esfand has 29 or 30. */
    @Override
    public int lengthOfMonth(int year, int month) {
        if (month <= 6) {
            return 31;
        }
        if (month <= 11) {
            return 30;
        }
        return isLeapYear(year) ? 30 : 29;
    }

    @Override
    public boolean isLeapYear(int year) {
        return leapOffset(year).leap == 0;
    }

    @Override
    public String monthName(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month out of range: " + month);
        }
        return MONTHS[month - 1];
    }

    // -----------------------------------------------------------------
    // Core algorithm
    // -----------------------------------------------------------------

    /**
     * Leap-year position and the Gregorian March day on which the given Jalali
     * year begins.
     *
     * @param leap   0 when {@code jy} is a leap year
     * @param gy     the Gregorian year containing that Nowruz
     * @param march  day of March on which 1 Farvardin falls
     */
    private record LeapOffset(int leap, int gy, int march) { }

    private LeapOffset leapOffset(int jy) {
        int gy = jy + 621;
        int leapJ = -14;
        int jp = BREAKS[0];

        if (jy < jp || jy >= BREAKS[BREAKS.length - 1]) {
            throw new IllegalArgumentException(
                    "Jalali year %d is outside the supported range %d..%d"
                            .formatted(jy, BREAKS[0], BREAKS[BREAKS.length - 1] - 1));
        }

        int jump = 0;
        for (int i = 1; i < BREAKS.length; i++) {
            int jm = BREAKS[i];
            jump = jm - jp;
            if (jy < jm) {
                break;
            }
            leapJ = leapJ + (jump / 33) * 8 + (jump % 33) / 4;
            jp = jm;
        }

        int n = jy - jp;
        leapJ = leapJ + (n / 33) * 8 + ((n % 33) + 3) / 4;
        if ((jump % 33) == 4 && (jump - n) == 4) {
            leapJ++;
        }

        int leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150;
        int march = 20 + leapJ - leapG;

        if ((jump - n) < 6) {
            n = n - jump + ((jump + 4) / 33) * 33;
        }
        int leap = (((n + 1) % 33) - 1) % 4;
        if (leap == -1) {
            leap = 4;
        }
        return new LeapOffset(leap, gy, march);
    }

    /** Gregorian calendar date to Julian Day Number. */
    private static int gregorianToJulianDay(int gy, int gm, int gd) {
        int d = ((gy + (gm - 8) / 6 + 100100) * 1461) / 4
                + (153 * ((gm + 9) % 12) + 2) / 5
                + gd - 34840408;
        return d - ((((gy + 100100 + (gm - 8) / 6) / 100) * 3) / 4) + 752;
    }

    /** Julian Day Number to Gregorian {@code [year, month, day]}. */
    private static int[] julianDayToGregorian(int jdn) {
        int j = 4 * jdn + 139361631;
        j = j + ((((4 * jdn + 183187720) / 146097) * 3) / 4) * 4 - 3908;
        int i = ((j % 1461) / 4) * 5 + 308;
        int gd = ((i % 153) / 5) + 1;
        // Modulo 12, not 4. The formula works in a March-based year, so
        // i/153 runs 2..13 across the twelve months; taking it modulo 4
        // yields correct months only until the fourth (April) and then
        // silently wraps — the kind of error that round-trips fine for a
        // Nowruz date and corrupts everything from Ordibehesht onward.
        int gm = ((i / 153) % 12) + 1;
        int gy = (j / 1461) - 100100 + ((8 - gm) / 6);
        return new int[]{gy, gm, gd};
    }

    private int jalaliToJulianDay(int jy, int jm, int jd) {
        LeapOffset r = leapOffset(jy);
        return gregorianToJulianDay(r.gy, 3, r.march)
                + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1;
    }

    private CalendarDate julianDayToJalali(int jdn) {
        int gy = julianDayToGregorian(jdn)[0];
        int jy = gy - 621;
        LeapOffset r = leapOffset(jy);
        int firstDayOfYear = gregorianToJulianDay(gy, 3, r.march);
        int k = jdn - firstDayOfYear;

        if (k >= 0) {
            if (k <= 185) {
                // Farvardin..Shahrivar — the six 31-day months.
                return new CalendarDate(jy, 1 + k / 31, (k % 31) + 1);
            }
            k -= 186;
        } else {
            // Before Nowruz: the date belongs to the previous Jalali year.
            jy--;
            k += 179;
            if (r.leap == 1) {
                k++;
            }
        }
        return new CalendarDate(jy, 7 + k / 30, (k % 30) + 1);
    }
}
