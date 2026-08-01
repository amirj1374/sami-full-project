package com.sami.app.common.scheduler.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.common.scheduler.domain.ScheduledJob;
import com.sami.app.common.scheduler.repository.JobExecutionRepository;
import com.sami.app.common.scheduler.service.JobRunner;
import com.sami.app.common.scheduler.service.JobService;
import com.sami.app.common.scheduler.spi.JobHandlerRegistry;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.common.scheduler.web.SchedulerDtos.CreateJobRequest;
import com.sami.app.common.scheduler.web.SchedulerDtos.ExecutionResponse;
import com.sami.app.common.scheduler.web.SchedulerDtos.HandlerResponse;
import com.sami.app.common.scheduler.web.SchedulerDtos.JobResponse;
import com.sami.app.common.scheduler.web.SchedulerDtos.StatusChangeRequest;
import com.sami.app.common.scheduler.web.SchedulerDtos.StatusResponse;
import com.sami.app.common.scheduler.web.SchedulerDtos.UpdateJobRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/scheduler")
@RequiredArgsConstructor
@Tag(name = "Scheduler", description = "Scheduled jobs, execution history and manual runs")
public class SchedulerController {

    private final JobService jobService;
    private final JobRunner jobRunner;
    private final JobExecutionRepository executionRepository;
    private final JobHandlerRegistry handlerRegistry;
    private final TenantContext tenantContext;

    @GetMapping("/jobs")
    @PreAuthorize("@authz.has('scheduler:view')")
    @Operation(summary = "List scheduled jobs")
    public ApiResponse<List<JobResponse>> jobs() {
        return ApiResponse.ok(jobService.list().stream().map(this::toResponse).toList());
    }

    @GetMapping("/jobs/{id}")
    @PreAuthorize("@authz.has('scheduler:view')")
    @Operation(summary = "Get a scheduled job")
    public ApiResponse<JobResponse> job(@PathVariable Long id) {
        return ApiResponse.ok(toResponse(jobService.get(id)));
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('scheduler:create')")
    @Operation(summary = "Create a scheduled job")
    public ApiResponse<JobResponse> create(@Valid @RequestBody CreateJobRequest request) {
        return ApiResponse.ok(toResponse(jobService.create(
                request.code(), request.name(), request.description(), request.handlerKey(),
                request.scheduleKind(), request.cronExpression(), request.intervalSeconds(),
                request.timezone(), request.config(), request.timeoutSeconds(),
                request.catchUp(), request.runAt())));
    }

    @PutMapping("/jobs/{id}")
    @PreAuthorize("@authz.has('scheduler:edit')")
    @Operation(summary = "Update a scheduled job")
    public ApiResponse<JobResponse> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateJobRequest request) {
        return ApiResponse.ok(toResponse(jobService.update(id, request.name(), request.description(),
                request.cronExpression(), request.intervalSeconds(), request.timezone(),
                request.config(), request.timeoutSeconds(), request.maxFailures(), request.catchUp())));
    }

    @PatchMapping("/jobs/{id}/status")
    @PreAuthorize("@authz.has('scheduler:edit')")
    @Operation(summary = "Activate or pause a job")
    public ApiResponse<JobResponse> changeStatus(@PathVariable Long id,
                                                 @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok(toResponse(jobService.changeStatus(id, request.status())));
    }

    @DeleteMapping("/jobs/{id}")
    @PreAuthorize("@authz.has('scheduler:delete')")
    @Operation(summary = "Delete a job")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/jobs/{id}/run")
    @PreAuthorize("@authz.has('scheduler:execute')")
    @Operation(summary = "Run a job now")
    public ApiResponse<ExecutionResponse> run(@PathVariable Long id) {
        jobService.get(id);
        return ApiResponse.ok(ExecutionResponse.from(jobRunner.runNow(id)));
    }

    @GetMapping("/jobs/{id}/executions")
    @PreAuthorize("@authz.has('scheduler:view')")
    @Operation(summary = "Execution history for a job")
    public ApiResponse<PageResponse<ExecutionResponse>> executions(
            @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(
                executionRepository.findAllByJobIdAndTenantIdOrderByStartedAtDesc(
                        jobService.get(id).getId(), tenantContext.requireTenantId(), pageable),
                ExecutionResponse::from));
    }

    @GetMapping("/executions")
    @PreAuthorize("@authz.has('scheduler:view')")
    @Operation(summary = "Recent executions across all jobs")
    public ApiResponse<PageResponse<ExecutionResponse>> allExecutions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(
                executionRepository.findAllByTenantIdOrderByStartedAtDesc(
                        tenantContext.requireTenantId(), pageable),
                ExecutionResponse::from));
    }

    @GetMapping("/statuses")
    @PreAuthorize("@authz.has('scheduler:view')")
    @Operation(summary = "Configurable job statuses")
    public ApiResponse<List<StatusResponse>> statuses() {
        return ApiResponse.ok(jobService.statuses().stream().map(StatusResponse::from).toList());
    }

    /**
     * The registered handler beans. Comparing this with the job list shows at a
     * glance whether a configured job has an implementation behind it.
     */
    @GetMapping("/handlers")
    @PreAuthorize("@authz.has('scheduler:view')")
    @Operation(summary = "Registered job handlers")
    public ApiResponse<List<HandlerResponse>> handlers() {
        Set<String> inUse = jobService.list().stream()
                .map(ScheduledJob::getHandlerKey).collect(Collectors.toSet());
        return ApiResponse.ok(handlerRegistry.all().stream()
                .map(h -> new HandlerResponse(h.key(), h.description(), inUse.contains(h.key())))
                .toList());
    }

    private JobResponse toResponse(ScheduledJob job) {
        return JobResponse.from(job, jobService.describe(job), jobService.handlerRegistered(job));
    }
}
