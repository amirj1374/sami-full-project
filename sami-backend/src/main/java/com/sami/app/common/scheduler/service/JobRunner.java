package com.sami.app.common.scheduler.service;

import com.sami.app.common.scheduler.SchedulerProperties;
import com.sami.app.common.scheduler.domain.JobExecution;
import com.sami.app.common.scheduler.domain.ScheduledJob;
import com.sami.app.common.scheduler.repository.ScheduledJobRepository;
import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.scheduler.spi.JobHandler;
import com.sami.app.common.scheduler.spi.JobHandlerRegistry;
import com.sami.app.common.scheduler.spi.JobResult;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The one place in this codebase that runs work on a clock.
 *
 * <p>A single poller claims due jobs and dispatches each to its
 * {@link JobHandler}. Modules never write their own {@code @Scheduled} methods —
 * they contribute a handler bean and a job row, which keeps every scheduled
 * activity listable, pausable, auditable and tenant-scoped.
 *
 * <p>All transactional state changes are delegated to {@link JobExecutionRecorder};
 * see that class for why they cannot live here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobRunner {

    private final ScheduledJobRepository jobRepository;
    private final JobHandlerRegistry handlerRegistry;
    private final JobExecutionRecorder recorder;
    private final SchedulerProperties properties;

    /** Handlers run here so a hung job cannot block the poller thread. */
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("sami-job-exec-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
    });

    // ---- Startup ------------------------------------------------------------

    /**
     * Scheduled after the application is fully up, not during bean creation, so a
     * misconfigured job can never prevent the system from starting.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!properties.enabled()) {
            log.info("Scheduler disabled (app.scheduler.enabled=false); no jobs will run");
            return;
        }
        int initialised = recorder.initialiseSchedules();
        log.info("Scheduler ready: {} job schedule(s) initialised, {} handler(s) registered: {}",
                initialised, handlerRegistry.keys().size(), handlerRegistry.keys());
    }

    // ---- Polling ------------------------------------------------------------

    @Scheduled(
            initialDelayString = "${app.scheduler.startup-delay-seconds:60}",
            fixedDelayString = "${app.scheduler.poll-seconds:30}",
            timeUnit = TimeUnit.SECONDS)
    public void poll() {
        if (!properties.enabled()) {
            return;
        }
        try {
            Instant now = Instant.now();
            List<ScheduledJob> due =
                    jobRepository.findDue(now, PageRequest.of(0, properties.batchSize()));

            for (ScheduledJob job : due) {
                Instant scheduledFor = job.getNextRunAt();
                if (recorder.claim(job.getId(), scheduledFor, now)) {
                    submit(job.getId(), job.getCode(), "SCHEDULED", scheduledFor);
                }
            }
        } catch (Exception e) {
            // The poller must never die: one bad cycle would silently stop every
            // scheduled activity in the system.
            log.error("Scheduler poll failed", e);
        }
    }

    private void submit(Long jobId, String jobCode, String triggerKind, Instant scheduledFor) {
        executor.submit(() -> {
            try {
                execute(jobId, triggerKind, scheduledFor);
            } catch (Exception e) {
                log.error("Job '{}' failed outside its handler", jobCode, e);
            }
        });
    }

    // ---- Execution ----------------------------------------------------------

    /**
     * Runs one job and records the outcome.
     *
     * <p>Deliberately not wrapped in a single transaction: a long sweep must not
     * hold one open for its duration, and the execution record must survive a
     * handler failure. Each state change commits independently via the recorder.
     */
    public JobExecution execute(Long jobId, String triggerKind, Instant scheduledFor) {
        ScheduledJob job = jobRepository.findById(jobId).orElseThrow();
        JobExecution execution = recorder.open(job, triggerKind);

        Optional<JobHandler> handler = handlerRegistry.find(job.getHandlerKey());
        if (handler.isEmpty()) {
            // Configuration references a bean that does not exist. Fail loudly and
            // let the failure counter pause the job — a silent skip is how a sweep
            // stops running without anyone noticing.
            return recorder.close(jobId, execution.getId(), "FAILED", null,
                    "No handler bean registered for key '%s'".formatted(job.getHandlerKey()),
                    null, 0L);
        }

        JobContext context = new JobContext(
                job.getCode(), job.getConfig(), job.getTenantId(),
                scheduledFor == null ? Instant.now() : scheduledFor,
                "MANUAL".equals(triggerKind), job.getLastRunAt());

        long start = System.nanoTime();
        try {
            JobResult result = runWithTimeout(handler.get(), context, job.getTimeoutSeconds());
            long ms = elapsedMs(start);
            return result.success()
                    ? recorder.close(jobId, execution.getId(), "SUCCEEDED",
                            result.outcome(), null, result.itemsProcessed(), ms)
                    : recorder.close(jobId, execution.getId(), "FAILED",
                            result.outcome(), result.outcome(), result.itemsProcessed(), ms);
        } catch (TimeoutException e) {
            return recorder.close(jobId, execution.getId(), "TIMED_OUT", null,
                    "Exceeded timeout of %ds".formatted(job.getTimeoutSeconds()), null,
                    elapsedMs(start));
        } catch (Exception e) {
            log.error("Job '{}' threw", job.getCode(), e);
            return recorder.close(jobId, execution.getId(), "FAILED", null,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), null, elapsedMs(start));
        }
    }

    /**
     * Manual run. Bypasses {@code allowsRun} — an operator may deliberately run a
     * paused job — but still records the execution and updates the counters.
     */
    public JobExecution runNow(Long jobId) {
        return execute(jobId, "MANUAL", Instant.now());
    }

    private JobResult runWithTimeout(JobHandler handler, JobContext context, int timeoutSeconds)
            throws Exception {
        CompletableFuture<JobResult> future =
                CompletableFuture.supplyAsync(() -> handler.execute(context), executor);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // Interrupt the handler; a well-behaved one stops. The run is recorded
            // as TIMED_OUT either way, so a handler that ignores interruption still
            // appears in the history rather than vanishing.
            future.cancel(true);
            throw e;
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
