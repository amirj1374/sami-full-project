package com.sami.app.common.scheduler.web;

import com.sami.app.common.scheduler.domain.JobExecution;
import com.sami.app.common.scheduler.domain.JobStatus;
import com.sami.app.common.scheduler.domain.ScheduledJob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

/** Request/response records for the scheduler API. */
public final class SchedulerDtos {

    private SchedulerDtos() {
    }

    public record JobResponse(Long id, String code, String name, String description,
                              String handlerKey, boolean handlerRegistered,
                              String scheduleKind, String cronExpression, Integer intervalSeconds,
                              String schedule, String timezone, Map<String, Object> config,
                              String statusCode, String statusName, boolean active,
                              Instant nextRunAt, Instant lastRunAt, String lastStatus,
                              Long lastDurationMs, int consecutiveFailures, int maxFailures,
                              int timeoutSeconds, boolean catchUp, boolean system) {

        public static JobResponse from(ScheduledJob j, String schedule, boolean handlerRegistered) {
            return new JobResponse(j.getId(), j.getCode(), j.getName(), j.getDescription(),
                    j.getHandlerKey(), handlerRegistered,
                    j.getScheduleKind(), j.getCronExpression(), j.getIntervalSeconds(),
                    schedule, j.getTimezone(), j.getConfig(),
                    j.getStatus().getCode(), j.getStatus().getName(), j.getStatus().isAllowsRun(),
                    j.getNextRunAt(), j.getLastRunAt(), j.getLastStatus(), j.getLastDurationMs(),
                    j.getConsecutiveFailures(), j.getMaxFailures(), j.getTimeoutSeconds(),
                    j.isCatchUp(), j.isSystem());
        }
    }

    public record ExecutionResponse(Long id, String executionNumber, String triggerKind,
                                    String status, Instant startedAt, Instant finishedAt,
                                    Long durationMs, String outcome, String errorMessage,
                                    Integer itemsProcessed, String actorEmail) {

        public static ExecutionResponse from(JobExecution e) {
            return new ExecutionResponse(e.getId(), e.getExecutionNumber(), e.getTriggerKind(),
                    e.getStatus(), e.getStartedAt(), e.getFinishedAt(), e.getDurationMs(),
                    e.getOutcome(), e.getErrorMessage(), e.getItemsProcessed(), e.getActorEmail());
        }
    }

    public record StatusResponse(Long id, String code, String name, boolean allowsRun,
                                 boolean paused, boolean failed) {

        public static StatusResponse from(JobStatus s) {
            return new StatusResponse(s.getId(), s.getCode(), s.getName(), s.isAllowsRun(),
                    s.isPausedState(), s.isFailedState());
        }
    }

    public record CreateJobRequest(@NotBlank @Size(max = 64) String code,
                                   @NotBlank @Size(max = 160) String name,
                                   @Size(max = 1000) String description,
                                   @NotBlank String handlerKey,
                                   String scheduleKind,
                                   String cronExpression,
                                   Integer intervalSeconds,
                                   String timezone,
                                   Map<String, Object> config,
                                   Integer timeoutSeconds,
                                   Boolean catchUp,
                                   Instant runAt) {
    }

    public record UpdateJobRequest(String name, String description, String cronExpression,
                                   Integer intervalSeconds, String timezone,
                                   Map<String, Object> config, Integer timeoutSeconds,
                                   Integer maxFailures, Boolean catchUp) {
    }

    public record StatusChangeRequest(@NotBlank String status) {
    }

    public record HandlerResponse(String key, String description, boolean inUse) {
    }
}
