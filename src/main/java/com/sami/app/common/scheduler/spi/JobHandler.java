package com.sami.app.common.scheduler.spi;

/**
 * A unit of scheduled work. One bean per {@code scheduled_jobs.handler_key}.
 *
 * <p>Handlers live in the module that owns the work — the file module owns
 * retention, licensing owns expiry — so the scheduler never depends on any
 * business or platform module. It only knows how to run a key.
 *
 * <p>Handlers must be <b>idempotent</b>: a job whose window was missed while the
 * process was down runs once at the next opportunity, and a manual run may
 * overlap a scheduled one.
 */
public interface JobHandler {

    /** Matches {@code scheduled_jobs.handler_key}. */
    String key();

    /** Human-readable description, surfaced in the job catalogue. */
    default String description() {
        return key();
    }

    JobResult execute(JobContext context);
}
