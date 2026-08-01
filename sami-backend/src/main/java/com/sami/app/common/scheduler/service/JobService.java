package com.sami.app.common.scheduler.service;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.scheduler.domain.JobStatus;
import com.sami.app.common.scheduler.domain.ScheduledJob;
import com.sami.app.common.scheduler.repository.JobStatusRepository;
import com.sami.app.common.scheduler.repository.ScheduledJobRepository;
import com.sami.app.common.scheduler.spi.JobHandlerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Job configuration: create, update, enable, pause, delete. */
@Service
@RequiredArgsConstructor
public class JobService {

    private final ScheduledJobRepository jobRepository;
    private final JobStatusRepository statusRepository;
    private final JobHandlerRegistry handlerRegistry;
    private final ScheduleCalculator calculator;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<ScheduledJob> list() {
        return jobRepository.findAllByTenantIdOrderByCodeAsc(tenantContext.requireTenantId());
    }

    @Transactional(readOnly = true)
    public ScheduledJob get(Long id) {
        return jobRepository.findByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Job", id));
    }

    @Transactional
    public ScheduledJob create(String code, String name, String description, String handlerKey,
                               String scheduleKind, String cronExpression, Integer intervalSeconds,
                               String timezone, Map<String, Object> config, Integer timeoutSeconds,
                               Boolean catchUp, Instant runAt) {
        Long tenantId = tenantContext.requireTenantId();
        if (jobRepository.findByTenantIdAndCode(tenantId, code).isPresent()) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "A job named '%s' already exists".formatted(code));
        }
        // Refuse a job whose handler does not exist, rather than discovering it at
        // 03:00 when the sweep silently fails.
        if (handlerRegistry.find(handlerKey).isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "No handler is registered for '%s'. Available: %s"
                            .formatted(handlerKey, String.join(", ", handlerRegistry.keys())));
        }

        JobStatus draft = statusRepository.findFirstByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default job status is configured"));

        ScheduledJob job = ScheduledJob.builder()
                .code(code).name(name).description(description)
                .handlerKey(handlerKey)
                .scheduleKind(scheduleKind == null ? "CRON" : scheduleKind)
                .cronExpression(cronExpression)
                .intervalSeconds(intervalSeconds)
                .timezone(timezone == null || timezone.isBlank() ? "UTC" : timezone)
                .config(config == null ? Map.of() : config)
                .status(draft)
                .timeoutSeconds(timeoutSeconds == null ? 300 : timeoutSeconds)
                .catchUp(Boolean.TRUE.equals(catchUp))
                .nextRunAt(runAt)
                .isSystem(false)
                
                .tenantId(tenantId)
                .build();

        calculator.validate(job);
        return jobRepository.save(job);
    }

    @Transactional
    public ScheduledJob update(Long id, String name, String description, String cronExpression,
                               Integer intervalSeconds, String timezone, Map<String, Object> config,
                               Integer timeoutSeconds, Integer maxFailures, Boolean catchUp) {
        ScheduledJob job = get(id);
        if (name != null) {
            job.setName(name);
        }
        if (description != null) {
            job.setDescription(description);
        }
        if (cronExpression != null) {
            job.setCronExpression(cronExpression);
        }
        if (intervalSeconds != null) {
            job.setIntervalSeconds(intervalSeconds);
        }
        if (timezone != null && !timezone.isBlank()) {
            job.setTimezone(timezone);
        }
        if (config != null) {
            job.setConfig(config);
        }
        if (timeoutSeconds != null) {
            job.setTimeoutSeconds(timeoutSeconds);
        }
        if (maxFailures != null) {
            job.setMaxFailures(maxFailures);
        }
        if (catchUp != null) {
            job.setCatchUp(catchUp);
        }

        calculator.validate(job);
        // The cadence may have changed, so recompute rather than leaving the job
        // on its old schedule until the next run.
        if (job.getStatus().isAllowsRun()) {
            job.setNextRunAt(calculator.next(job, Instant.now(), null));
        }
        return jobRepository.save(job);
    }

    /** Activates or pauses a job. Activating recomputes the next occurrence. */
    @Transactional
    public ScheduledJob changeStatus(Long id, String statusCode) {
        ScheduledJob job = get(id);
        JobStatus status = statusRepository.findByCode(statusCode)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unknown job status: " + statusCode));

        job.setStatus(status);
        if (status.isAllowsRun()) {
            calculator.validate(job);
            job.setNextRunAt(calculator.first(job, Instant.now()));
            // Clear the counter so a previously failed job gets a clean run.
            job.setConsecutiveFailures(0);
        } else {
            job.setNextRunAt(null);
        }
        return jobRepository.save(job);
    }

    @Transactional
    public void delete(Long id) {
        ScheduledJob job = get(id);
        if (job.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "System jobs cannot be deleted; pause them instead");
        }
        jobRepository.delete(job);
    }

    @Transactional(readOnly = true)
    public List<JobStatus> statuses() {
        return statusRepository.findAllByOrderByDisplayOrderAsc();
    }

    public String describe(ScheduledJob job) {
        return calculator.describe(job);
    }

    public boolean handlerRegistered(ScheduledJob job) {
        return handlerRegistry.find(job.getHandlerKey()).isPresent();
    }
}
