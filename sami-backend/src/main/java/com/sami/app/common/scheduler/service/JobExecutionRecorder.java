package com.sami.app.common.scheduler.service;

import com.sami.app.common.scheduler.domain.JobExecution;
import com.sami.app.common.scheduler.domain.ScheduledJob;
import com.sami.app.common.scheduler.repository.JobExecutionRepository;
import com.sami.app.common.scheduler.repository.JobStatusRepository;
import com.sami.app.common.scheduler.repository.ScheduledJobRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * All transactional state changes for job execution.
 *
 * <p>Deliberately a separate bean from {@link JobRunner}. Spring's
 * {@code @Transactional} is proxy-based, so a method annotated on the runner and
 * called from the runner's own poll loop would silently run with no transaction
 * at all — the claim would not be atomic and the {@code REQUIRES_NEW} execution
 * records would not survive a handler failure. Crossing a bean boundary is what
 * makes the annotations effective.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionRecorder {

    private final ScheduledJobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final JobStatusRepository statusRepository;
    private final ScheduleCalculator calculator;

    /**
     * Compare-and-swap claim: advances {@code next_run_at} only if it still holds
     * the value we read. The instance whose update affects a row wins; every
     * other instance sees zero and skips. The row update is the lock, so running
     * several application instances needs no external coordination.
     *
     * @return true if this instance won the claim
     */
    @Transactional
    public boolean claim(Long jobId, Instant expectedRunAt, Instant now) {
        ScheduledJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return false;
        }
        Instant next;
        try {
            Instant base = expectedRunAt.isBefore(now)
                    ? calculator.afterMissedWindow(job, now)
                    : expectedRunAt;
            next = calculator.next(job, base, null);
        } catch (Exception e) {
            log.error("Cannot compute next run for job '{}': {}", job.getCode(), e.toString());
            return false;
        }
        // A ONCE job has no next occurrence; park it ahead so the poller does not
        // re-select it before completion marks it done.
        Instant claimed = next == null ? now.plusSeconds(86_400) : next;
        return jobRepository.claim(jobId, expectedRunAt, claimed) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobExecution open(ScheduledJob job, String triggerKind) {
        return executionRepository.save(JobExecution.builder()
                .jobId(job.getId())
                .executionNumber("JOB-%08d".formatted(executionRepository.nextExecutionSequence()))
                .triggerKind(triggerKind)
                .status("RUNNING")
                .startedAt(Instant.now())
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .tenantId(job.getTenantId())
                .build());
    }

    /**
     * Records the outcome and updates the job's counters.
     *
     * <p>{@code REQUIRES_NEW} so the record survives even when the surrounding
     * work failed — an execution that vanishes on failure is worse than no
     * history at all.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobExecution close(Long jobId, Long executionId, String status, String outcome,
                              String error, Integer items, long durationMs) {
        JobExecution execution = executionRepository.findById(executionId).orElseThrow();
        Instant finished = Instant.now();

        execution.setStatus(status);
        execution.setFinishedAt(finished);
        execution.setDurationMs(durationMs);
        execution.setOutcome(truncate(outcome, 2000));
        execution.setErrorMessage(truncate(error, 4000));
        execution.setItemsProcessed(items);
        executionRepository.save(execution);

        ScheduledJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return execution;
        }
        job.setLastRunAt(finished);
        job.setLastStatus(status);
        job.setLastDurationMs(durationMs);

        if ("SUCCEEDED".equals(status)) {
            job.setConsecutiveFailures(0);
        } else {
            job.setConsecutiveFailures(job.getConsecutiveFailures() + 1);
            if (job.getConsecutiveFailures() >= job.getMaxFailures()) {
                // Auto-pause: a broken handler must not retry indefinitely.
                statusRepository.findFirstByIsFailedStateTrue().ifPresent(job::setStatus);
                log.warn("Job '{}' paused after {} consecutive failures",
                        job.getCode(), job.getConsecutiveFailures());
            }
        }

        // FIXED_DELAY measures from completion, so its next run is only knowable here.
        if ("FIXED_DELAY".equals(job.getScheduleKind())) {
            job.setNextRunAt(calculator.next(job, finished, finished));
        }
        if ("ONCE".equals(job.getScheduleKind())) {
            job.setNextRunAt(null);
            statusRepository.findByCode("completed").ifPresent(job::setStatus);
        }

        jobRepository.save(job);
        return execution;
    }

    /**
     * Gives {@code next_run_at} to jobs that have never been scheduled. An
     * invalid schedule is logged and skipped rather than thrown, so one bad job
     * cannot stop the others from starting.
     */
    @Transactional
    public int initialiseSchedules() {
        int initialised = 0;
        for (ScheduledJob job : jobRepository.findAll()) {
            if (job.getNextRunAt() == null && job.getStatus().isAllowsRun()) {
                try {
                    calculator.validate(job);
                    job.setNextRunAt(calculator.first(job, Instant.now()));
                    jobRepository.save(job);
                    initialised++;
                } catch (Exception e) {
                    log.warn("Job '{}' has an invalid schedule and was not started: {}",
                            job.getCode(), e.getMessage());
                }
            }
        }
        return initialised;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
