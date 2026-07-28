package com.sami.app.calendar;

import com.sami.app.calendar.spi.CalendarDate;
import com.sami.app.calendar.spi.GregorianCalendarSystem;
import com.sami.app.calendar.spi.PersianCalendarSystem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Solar Hijri conversion underpins every holiday and working-day
 * calculation in the scheduler, and a one-day error would be invisible until a
 * customer arrived on a closed Nowruz. These anchors are externally verifiable
 * against the official Iranian calendar.
 */
class PersianCalendarTest {

    private final PersianCalendarSystem persian = new PersianCalendarSystem();
    private final GregorianCalendarSystem gregorian = new GregorianCalendarSystem();

    @Nested
    @DisplayName("known anchor dates")
    class Anchors {

        @ParameterizedTest(name = "{0}-{1}-{2} Gregorian = {3}-{4}-{5} Jalali")
        @CsvSource({
                // Nowruz — 1 Farvardin. The single most consequential date.
                "2020, 3, 20,  1399, 1, 1",
                "2021, 3, 21,  1400, 1, 1",
                "2024, 3, 20,  1403, 1, 1",
                "2026, 3, 21,  1405, 1, 1",
                // Unix epoch, a widely published cross-check.
                "1970, 1, 1,   1348, 10, 11",
                // Day before Nowruz — last day of Esfand, exercises the
                // negative-k branch of the conversion.
                "2026, 3, 20,  1404, 12, 29",
                // Mid-year, all four seasons.
                "2026, 7, 20,  1405, 4, 29",
                "2025, 1, 15,  1403, 10, 26",
                "2023, 9, 23,  1402, 7, 1",
        })
        void convertBothWays(int gy, int gm, int gd, int jy, int jm, int jd) {
            LocalDate gregorianDate = LocalDate.of(gy, gm, gd);
            CalendarDate jalali = new CalendarDate(jy, jm, jd);

            assertThat(persian.fromGregorian(gregorianDate)).isEqualTo(jalali);
            assertThat(persian.toGregorian(jalali)).isEqualTo(gregorianDate);
        }
    }

    @Nested
    @DisplayName("round-tripping")
    class RoundTrip {

        /**
         * Every day across a span covering leap and common years in both
         * chronologies. A drift of even one day anywhere fails here.
         */
        @Test
        void isLosslessOverTwentyYears() {
            LocalDate date = LocalDate.of(2015, 1, 1);
            LocalDate end = LocalDate.of(2035, 1, 1);
            while (date.isBefore(end)) {
                CalendarDate jalali = persian.fromGregorian(date);
                assertThat(persian.toGregorian(jalali))
                        .as("round trip for %s (Jalali %s)", date, jalali.iso())
                        .isEqualTo(date);
                date = date.plusDays(1);
            }
        }

        @Test
        void daysAdvanceMonotonically() {
            LocalDate date = LocalDate.of(2024, 1, 1);
            CalendarDate previous = persian.fromGregorian(date);
            for (int i = 1; i < 1500; i++) {
                CalendarDate current = persian.fromGregorian(date.plusDays(i));
                boolean advanced = current.year() > previous.year()
                        || (current.year() == previous.year() && current.month() > previous.month())
                        || (current.year() == previous.year() && current.month() == previous.month()
                            && current.day() == previous.day() + 1);
                assertThat(advanced)
                        .as("%s must follow %s", current.iso(), previous.iso())
                        .isTrue();
                previous = current;
            }
        }
    }

    @Nested
    @DisplayName("leap years and month lengths")
    class Leap {

        /**
         * Officially designated Jalali leap years. 1403 in particular is one
         * the naive 33-year-cycle approximation gets wrong, which is why that
         * shortcut is not used.
         */
        @ParameterizedTest
        @CsvSource({"1399, true", "1403, true", "1400, false", "1401, false",
                    "1402, false", "1404, false", "1408, true"})
        void identifiesLeapYears(int year, boolean leap) {
            assertThat(persian.isLeapYear(year)).isEqualTo(leap);
        }

        @Test
        void esfandHasThirtyDaysOnlyInLeapYears() {
            assertThat(persian.lengthOfMonth(1403, 12)).isEqualTo(30);
            assertThat(persian.lengthOfMonth(1404, 12)).isEqualTo(29);
        }

        @Test
        void firstSixMonthsAreLongerThanTheRest() {
            for (int m = 1; m <= 6; m++) {
                assertThat(persian.lengthOfMonth(1404, m)).isEqualTo(31);
            }
            for (int m = 7; m <= 11; m++) {
                assertThat(persian.lengthOfMonth(1404, m)).isEqualTo(30);
            }
        }

        @Test
        void rejectsADayThatDoesNotExist() {
            assertThatThrownBy(() -> persian.toGregorian(new CalendarDate(1404, 12, 30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not exist");
        }
    }

    @Nested
    @DisplayName("recurring holiday resolution")
    class Recurrence {

        /**
         * The reason recurrences are stored as month/day rather than as a
         * date: Nowruz lands on a different Gregorian day each year.
         *
         * <p>The expected values follow the official Iranian rule — the year
         * begins on the day the vernal equinox falls before true noon in
         * Tehran, otherwise the following day. 2024's equinox was 06:36 local
         * (before noon → 20 March); 2025's was 12:31 and 2026's 18:16 (both
         * after noon → 21 March). The arithmetic algorithm reproduces this.
         */
        @Test
        void nowruzMovesAcrossGregorianYears() {
            assertThat(persian.resolveRecurrence(2024, 1, 1)).isEqualTo(LocalDate.of(2024, 3, 20));
            assertThat(persian.resolveRecurrence(2025, 1, 1)).isEqualTo(LocalDate.of(2025, 3, 21));
            assertThat(persian.resolveRecurrence(2026, 1, 1)).isEqualTo(LocalDate.of(2026, 3, 21));
        }

        @Test
        void resolvesALateYearMonth() {
            // 11 Dey 1404 — a winter recurrence, which falls in the Gregorian
            // year AFTER the one containing that Jalali new year.
            LocalDate resolved = persian.resolveRecurrence(2026, 10, 11);
            assertThat(resolved).isNotNull();
            assertThat(persian.fromGregorian(resolved).month()).isEqualTo(10);
            assertThat(persian.fromGregorian(resolved).day()).isEqualTo(11);
            assertThat(resolved.getYear()).isEqualTo(2026);
        }

        @Test
        void gregorianLeapDayHasNoOccurrenceInACommonYear() {
            assertThat(gregorian.resolveRecurrence(2024, 2, 29)).isEqualTo(LocalDate.of(2024, 2, 29));
            assertThat(gregorian.resolveRecurrence(2025, 2, 29)).isNull();
        }
    }

    @Test
    void gregorianSystemIsTheIdentityConversion() {
        LocalDate date = LocalDate.of(2026, 7, 20);
        assertThat(gregorian.toGregorian(gregorian.fromGregorian(date))).isEqualTo(date);
        assertThat(gregorian.fromGregorian(date).iso()).isEqualTo("2026-07-20");
    }
}
