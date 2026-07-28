package com.sami.app;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.scheduler.domain.ScheduledJob;
import com.sami.app.common.scheduler.service.ScheduleCalculator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ScheduleCalculator}. Pure: no Spring context, no
 * database. These cover the cases where scheduling quietly goes wrong —
 * daylight saving, missed windows after downtime, and the difference between
 * fixed-rate and fixed-delay.
 */
class ScheduleCalculatorTest {

    private final ScheduleCalculator calculator = new ScheduleCalculator();

    private static ScheduledJob cron(String expression, String timezone) {
        return ScheduledJob.builder()
                .code("job").name("Job").handlerKey("h")
                .scheduleKind("CRON").cronExpression(expression)
                .timezone(timezone == null ? "UTC" : timezone)
                .build();
    }

    private static ScheduledJob interval(String kind, int seconds) {
        return ScheduledJob.builder()
                .code("job").name("Job").handlerKey("h")
                .scheduleKind(kind).intervalSeconds(seconds).timezone("UTC")
                .build();
    }

    // ---- Cron ---------------------------------------------------------------

    @Test
    void cronProducesTheNextDailyOccurrence() {
        Instant from = ZonedDateTime.of(
                LocalDateTime.of(2026, 7, 19, 12, 0), ZoneId.of("UTC")).toInstant();

        Instant next = calculator.next(cron("0 30 2 * * *", "UTC"), from, null);

        assertThat(next).isEqualTo(ZonedDateTime.of(
                LocalDateTime.of(2026, 7, 20, 2, 30), ZoneId.of("UTC")).toInstant());
    }

    @Test
    void cronIsEvaluatedInTheJobsOwnTimeZone() {
        ZoneId tehran = ZoneId.of("Asia/Tehran");
        Instant from = ZonedDateTime.of(
                LocalDateTime.of(2026, 7, 19, 12, 0), tehran).toInstant();

        Instant next = calculator.next(cron("0 0 3 * * *", "Asia/Tehran"), from, null);

        // The invariant is local 03:00 in Tehran, not 03:00 UTC.
        assertThat(next.atZone(tehran).getHour()).isEqualTo(3);
        assertThat(next.atZone(tehran).toLocalDate())
                .isEqualTo(java.time.LocalDate.of(2026, 7, 20));
    }

    @Test
    void aDailyJobStaysAtLocalTimeAcrossADaylightSavingTransition() {
        // 8 March 2026 is the US spring-forward date. A 03:00 daily sweep must stay
        // at local 03:00 rather than drifting an hour, which is what happens when
        // cron is evaluated in UTC and the offset changes underneath it.
        ZoneId newYork = ZoneId.of("America/New_York");
        Instant beforeTransition = ZonedDateTime.of(
                LocalDateTime.of(2026, 3, 7, 12, 0), newYork).toInstant();

        Instant next = calculator.next(cron("0 0 3 * * *", "America/New_York"),
                beforeTransition, null);

        assertThat(next.atZone(newYork).getHour()).isEqualTo(3);
        assertThat(next.atZone(newYork).toLocalDate())
                .isEqualTo(java.time.LocalDate.of(2026, 3, 8));
    }

    // ---- Interval kinds -----------------------------------------------------

    @Test
    void fixedRateMeasuresFromTheScheduledTimeNotFromCompletion() {
        Instant scheduled = Instant.parse("2026-07-19T10:00:00Z");
        Instant completed = Instant.parse("2026-07-19T10:04:00Z");

        Instant next = calculator.next(interval("FIXED_RATE", 300), scheduled, completed);

        // 10:00 + 5m, ignoring that the run took four minutes.
        assertThat(next).isEqualTo(Instant.parse("2026-07-19T10:05:00Z"));
    }

    @Test
    void fixedDelayMeasuresFromCompletion() {
        Instant scheduled = Instant.parse("2026-07-19T10:00:00Z");
        Instant completed = Instant.parse("2026-07-19T10:04:00Z");

        Instant next = calculator.next(interval("FIXED_DELAY", 300), scheduled, completed);

        // 10:04 + 5m — a slow run pushes the next one out, which is the point.
        assertThat(next).isEqualTo(Instant.parse("2026-07-19T10:09:00Z"));
    }

    @Test
    void fixedDelayFallsBackToTheScheduledTimeWhenCompletionIsUnknown() {
        Instant scheduled = Instant.parse("2026-07-19T10:00:00Z");

        assertThat(calculator.next(interval("FIXED_DELAY", 60), scheduled, null))
                .isEqualTo(Instant.parse("2026-07-19T10:01:00Z"));
    }

    @Test
    void aOnceJobHasNoNextOccurrence() {
        ScheduledJob once = ScheduledJob.builder()
                .code("j").name("J").handlerKey("h").scheduleKind("ONCE")
                .nextRunAt(Instant.parse("2026-07-20T09:00:00Z")).timezone("UTC").build();

        assertThat(calculator.next(once, Instant.now(), null)).isNull();
        // Its first run is the time it was created with — that value IS the schedule.
        assertThat(calculator.first(once, Instant.now()))
                .isEqualTo(Instant.parse("2026-07-20T09:00:00Z"));
    }

    // ---- Missed windows -----------------------------------------------------

    @Test
    void aMissedWindowRunsOnceWhenCatchUpIsOff() {
        // A nightly sweep offline for a week must run once, not seven times.
        ScheduledJob job = cron("0 0 3 * * *", "UTC");
        job.setCatchUp(false);
        job.setNextRunAt(Instant.parse("2026-07-12T03:00:00Z"));
        Instant now = Instant.parse("2026-07-19T09:00:00Z");

        assertThat(calculator.afterMissedWindow(job, now)).isEqualTo(now);
    }

    @Test
    void aMissedWindowIsReplayedFromItsOriginalTimeWhenCatchUpIsOn() {
        ScheduledJob job = cron("0 0 3 * * *", "UTC");
        job.setCatchUp(true);
        Instant missed = Instant.parse("2026-07-12T03:00:00Z");
        job.setNextRunAt(missed);

        assertThat(calculator.afterMissedWindow(job, Instant.parse("2026-07-19T09:00:00Z")))
                .isEqualTo(missed);
    }

    // ---- Validation ---------------------------------------------------------

    @Test
    void acceptsAValidSixFieldCronExpression() {
        assertThatCode(() -> calculator.validate(cron("0 */5 * * * *", "UTC")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnInvalidCronExpression() {
        assertThatThrownBy(() -> calculator.validate(cron("not a cron", "UTC")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid cron expression");
    }

    @Test
    void rejectsACronJobWithNoExpression() {
        assertThatThrownBy(() -> calculator.validate(cron(null, "UTC")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("requires a cron expression");
    }

    @Test
    void rejectsAnIntervalJobWithNoOrNegativeInterval() {
        assertThatThrownBy(() -> calculator.validate(interval("FIXED_RATE", 0)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("positive interval");
    }

    @Test
    void rejectsAnUnknownTimeZone() {
        assertThatThrownBy(() -> calculator.validate(cron("0 0 3 * * *", "Mars/Olympus")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown time zone");
    }

    @Test
    void rejectsAnUnknownScheduleKind() {
        ScheduledJob job = ScheduledJob.builder()
                .code("j").name("J").handlerKey("h").scheduleKind("HOURLY_ISH").timezone("UTC").build();

        assertThatThrownBy(() -> calculator.validate(job))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown schedule kind");
    }

    @Test
    void describesEachScheduleKindReadably() {
        assertThat(calculator.describe(cron("0 0 3 * * *", "Asia/Tehran")))
                .contains("cron").contains("Asia/Tehran");
        assertThat(calculator.describe(interval("FIXED_RATE", 3600))).isEqualTo("every 1h");
        assertThat(calculator.describe(interval("FIXED_DELAY", 300)))
                .isEqualTo("5m after each completion");
    }
}
