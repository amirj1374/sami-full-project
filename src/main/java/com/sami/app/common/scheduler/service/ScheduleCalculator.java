package com.sami.app.common.scheduler.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.scheduler.domain.ScheduledJob;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Works out when a job should next run.
 *
 * <p>Pure logic with no database or Spring context beyond the bean declaration,
 * so the scheduling rules — including the missed-window and time-zone cases —
 * are unit-testable in isolation.
 *
 * <p>Cron uses Spring's {@link CronExpression} (six fields, seconds first),
 * which is part of the framework already in use. No new dependency.
 */
@Component
public class ScheduleCalculator {

    /**
     * The next run after {@code from}.
     *
     * @param completedAt when the run that just finished ended; used by
     *                    FIXED_DELAY, which measures from completion rather than
     *                    from the scheduled time
     */
    public Instant next(ScheduledJob job, Instant from, Instant completedAt) {
        return switch (job.getScheduleKind()) {
            case "CRON" -> nextCron(job, from);
            case "FIXED_RATE" -> from.plusSeconds(job.getIntervalSeconds());
            case "FIXED_DELAY" -> (completedAt == null ? from : completedAt)
                    .plusSeconds(job.getIntervalSeconds());
            // A ONCE job has no next occurrence; the runner marks it completed.
            case "ONCE" -> null;
            default -> throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Unknown schedule kind: " + job.getScheduleKind());
        };
    }

    /**
     * The first run for a job that has never run.
     *
     * <p>A ONCE job keeps the {@code nextRunAt} it was created with — that value
     * *is* its schedule.
     */
    public Instant first(ScheduledJob job, Instant now) {
        if ("ONCE".equals(job.getScheduleKind())) {
            return job.getNextRunAt();
        }
        return next(job, now, null);
    }

    /**
     * Recovers the schedule after downtime.
     *
     * <p>When {@code catchUp} is false — the default, and the correct choice for
     * idempotent sweeps — a job whose window passed while the process was down
     * runs once now and then resumes its normal cadence, rather than replaying
     * every missed occurrence. That distinction matters: a nightly sweep offline
     * for a week should run once, not seven times.
     */
    public Instant afterMissedWindow(ScheduledJob job, Instant now) {
        if (job.isCatchUp()) {
            return job.getNextRunAt();
        }
        return now;
    }

    /**
     * Validates a schedule before it is saved, so an unrunnable job cannot be
     * created through the API. The database CHECK constraint enforces the same
     * rules; this produces a usable error message instead of a constraint violation.
     */
    public void validate(ScheduledJob job) {
        switch (job.getScheduleKind()) {
            case "CRON" -> {
                if (job.getCronExpression() == null || job.getCronExpression().isBlank()) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED,
                            "A CRON job requires a cron expression");
                }
                if (!CronExpression.isValidExpression(job.getCronExpression())) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED,
                            "Invalid cron expression '%s'. Expected six fields: "
                                    .formatted(job.getCronExpression())
                                    + "second minute hour day-of-month month day-of-week");
                }
            }
            case "FIXED_RATE", "FIXED_DELAY" -> {
                if (job.getIntervalSeconds() == null || job.getIntervalSeconds() <= 0) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED,
                            "A %s job requires a positive interval".formatted(job.getScheduleKind()));
                }
            }
            case "ONCE" -> {
                if (job.getNextRunAt() == null) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED,
                            "A ONCE job requires a scheduled time");
                }
            }
            default -> throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Unknown schedule kind '%s'. Expected CRON, FIXED_RATE, FIXED_DELAY or ONCE"
                            .formatted(job.getScheduleKind()));
        }

        if (job.getTimezone() != null && !job.getTimezone().isBlank()) {
            try {
                ZoneId.of(job.getTimezone());
            } catch (Exception e) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unknown time zone: " + job.getTimezone());
            }
        }
    }

    /** Human-readable cadence, for the job list. */
    public String describe(ScheduledJob job) {
        return switch (job.getScheduleKind()) {
            case "CRON" -> "cron " + job.getCronExpression() + " (" + job.getTimezone() + ")";
            case "FIXED_RATE" -> "every " + humanise(job.getIntervalSeconds());
            case "FIXED_DELAY" -> humanise(job.getIntervalSeconds()) + " after each completion";
            case "ONCE" -> "once at " + job.getNextRunAt();
            default -> job.getScheduleKind();
        };
    }

    private Instant nextCron(ScheduledJob job, Instant from) {
        ZoneId zone = ZoneId.of(job.getTimezone() == null || job.getTimezone().isBlank()
                ? "UTC" : job.getTimezone());
        CronExpression cron = CronExpression.parse(job.getCronExpression());

        // Evaluated in the job's own zone so a "03:00 daily" sweep stays at local
        // 03:00 across daylight-saving transitions rather than drifting an hour.
        ZonedDateTime next = cron.next(ZonedDateTime.ofInstant(from, zone));
        return next == null ? null : next.toInstant();
    }

    private String humanise(Integer seconds) {
        if (seconds == null) {
            return "?";
        }
        Duration d = Duration.ofSeconds(seconds);
        if (d.toHours() > 0) {
            return d.toHours() + "h";
        }
        if (d.toMinutes() > 0) {
            return d.toMinutes() + "m";
        }
        return seconds + "s";
    }
}
