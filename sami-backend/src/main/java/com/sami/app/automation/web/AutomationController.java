package com.sami.app.automation.web;

import com.sami.app.automation.dto.AutomationDtos.ActionDescriptorResponse;
import com.sami.app.automation.dto.AutomationDtos.ExecutionLogResponse;
import com.sami.app.automation.dto.AutomationDtos.ExecutionResponse;
import com.sami.app.automation.dto.AutomationDtos.RuleFilter;
import com.sami.app.automation.dto.AutomationDtos.RuleRequest;
import com.sami.app.automation.dto.AutomationDtos.RuleResponse;
import com.sami.app.automation.dto.AutomationDtos.RunRequest;
import com.sami.app.automation.dto.AutomationDtos.StatusChangeRequest;
import com.sami.app.automation.dto.AutomationDtos.StatusResponse;
import com.sami.app.automation.dto.AutomationDtos.TriggerDescriptorResponse;
import com.sami.app.automation.dto.AutomationDtos.FailureResponse;
import com.sami.app.automation.dto.AutomationDtos.MonitoringResponse;
import com.sami.app.automation.service.AutomationService;
import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/**
 * Business Process Automation API. Rule configuration and execution are entirely
 * data-driven; adding triggers/actions never changes this controller.
 */
@RestController
@RequestMapping("/api/v1/automations")
@RequiredArgsConstructor
@Tag(name = "Automation", description = "Business process automation rules, workflows and executions")
public class AutomationController {

    private final AutomationService service;

    @GetMapping
    @PreAuthorize("@authz.has('automation:view')")
    @Operation(summary = "List automation rules (paginated, filterable)")
    public ApiResponse<PageResponse<RuleResponse>> list(
            RuleFilter filter,
            @PageableDefault(size = 20, sort = "priority") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(service.list(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('automation:view')")
    @Operation(summary = "Get an automation rule with its actions")
    public ApiResponse<RuleResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('automation:create')")
    @Operation(summary = "Create an automation rule")
    public ApiResponse<RuleResponse> create(@Valid @RequestBody RuleRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('automation:edit')")
    @Operation(summary = "Update an automation rule")
    public ApiResponse<RuleResponse> update(@PathVariable Long id, @Valid @RequestBody RuleRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.has('automation:manage-status')")
    @Operation(summary = "Change an automation rule's status")
    public ApiResponse<RuleResponse> changeStatus(@PathVariable Long id,
                                                  @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok(service.changeStatus(id, request.statusCode(), request.expectedVersion()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('automation:delete')")
    @Operation(summary = "Delete an automation rule")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/run")
    @PreAuthorize("@authz.has('automation:execute')")
    @Operation(summary = "Manually execute an automation rule")
    public ApiResponse<Void> run(@PathVariable Long id, @RequestBody(required = false) RunRequest request) {
        service.run(id, request);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/executions")
    @PreAuthorize("@authz.has('automation:view')")
    @Operation(summary = "List a rule's executions (paginated)")
    public ApiResponse<PageResponse<ExecutionResponse>> executions(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(service.executions(id, pageable)));
    }

    @GetMapping("/executions/{executionId}/logs")
    @PreAuthorize("@authz.has('automation:view')")
    @Operation(summary = "Step-by-step log of one execution")
    public ApiResponse<List<ExecutionLogResponse>> executionLogs(@PathVariable Long executionId) {
        return ApiResponse.ok(service.executionLogs(executionId));
    }

    @GetMapping("/statuses")
    @PreAuthorize("@authz.has('automation:view')")
    @Operation(summary = "List configurable automation statuses")
    public ApiResponse<List<StatusResponse>> statuses() {
        return ApiResponse.ok(service.statuses());
    }

    @GetMapping("/triggers")
    @PreAuthorize("@authz.has('automation:view')")
    @Operation(summary = "List available trigger providers (plugin catalog)")
    public ApiResponse<List<TriggerDescriptorResponse>> triggers() {
        return ApiResponse.ok(service.triggers());
    }

    @GetMapping("/actions")
    @PreAuthorize("@authz.has('automation:view')")
    @Operation(summary = "List available action providers (plugin catalog)")
    public ApiResponse<List<ActionDescriptorResponse>> actions() {
        return ApiResponse.ok(service.actions());
    }

    @GetMapping("/monitoring")
    @PreAuthorize("@authz.has('automation:view')")
    @Operation(summary = "Automation execution and failure monitoring summary")
    public ApiResponse<MonitoringResponse> monitoring() {
        return ApiResponse.ok(service.monitoring());
    }

    @GetMapping("/failures")
    @PreAuthorize("@authz.has('automation:view')")
    @Operation(summary = "List automation failures requiring review")
    public ApiResponse<PageResponse<FailureResponse>> failures(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean resolved,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(service.failures(resolved, pageable)));
    }

    @PostMapping("/failures/{id}/retry")
    @PreAuthorize("@authz.has('automation:execute')")
    @Operation(summary = "Retry an automation failure")
    public ApiResponse<FailureResponse> retryFailure(@PathVariable Long id) {
        return ApiResponse.ok(service.retryFailure(id));
    }

    @PostMapping("/failures/{id}/resolve")
    @PreAuthorize("@authz.has('automation:manage-status')")
    @Operation(summary = "Resolve an automation failure after review")
    public ApiResponse<FailureResponse> resolveFailure(@PathVariable Long id) {
        return ApiResponse.ok(service.resolveFailure(id));
    }

    @GetMapping("/configuration/export")
    @PreAuthorize("@authz.has('automation:export')")
    @Operation(summary = "Export tenant automation configuration")
    public ApiResponse<List<RuleResponse>> exportConfiguration() {
        return ApiResponse.ok(service.exportConfiguration());
    }

    @PostMapping("/configuration/import")
    @PreAuthorize("@authz.has('automation:import')")
    @Operation(summary = "Import validated tenant automation configuration")
    public ApiResponse<List<RuleResponse>> importConfiguration(
            @Valid @RequestBody List<@Valid RuleRequest> rules) {
        return ApiResponse.ok(service.importConfiguration(rules));
    }

    @GetMapping(value = "/reports/executions.csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("@authz.has('automation:report')")
    @Operation(summary = "Export automation execution report")
    public ResponseEntity<byte[]> executionReport() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''automation-executions.csv")
                .body(service.executionReportCsv());
    }
}
