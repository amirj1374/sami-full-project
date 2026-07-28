package com.sami.app.automation.dto;

import com.sami.app.automation.domain.AutomationAction;
import com.sami.app.automation.domain.AutomationExecution;
import com.sami.app.automation.domain.AutomationExecutionLog;
import com.sami.app.automation.domain.AutomationRule;
import com.sami.app.automation.domain.AutomationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request/response records for the automation module (all wrapped in ApiResponse). */
public final class AutomationDtos {

    private AutomationDtos() {
    }

    // ---- Requests -----------------------------------------------------------

    public record ActionRequest(
            int stepOrder,
            @NotBlank String actionType,
            String name,
            Map<String, Object> config,
            Map<String, Object> stepCondition,
            String runMode,
            boolean continueOnError,
            int delaySeconds,
            int retryCount,
            Integer timeoutSeconds
    ) {
    }

    public record RuleRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$",
                    message = "code must be a lowercase slug") String code,
            @NotBlank String name,
            String description,
            String category,
            Integer priority,
            Long companyId,
            Long branchId,
            @NotBlank String statusCode,
            @NotBlank String triggerType,
            Map<String, Object> triggerConfig,
            Map<String, Object> conditionConfig,
            Map<String, Object> executionPolicy,
            boolean allowRecursion,
            Integer maxExecutions,
            @Valid List<ActionRequest> actions,
            Long expectedVersion
    ) {
    }

    public record RunRequest(String entityType, Long entityId, Map<String, Object> data) {
    }

    public record StatusChangeRequest(@NotBlank String statusCode, Long expectedVersion) {
    }

    // ---- Responses ----------------------------------------------------------

    public record ActionResponse(
            Long id, int stepOrder, String actionType, String name, Map<String, Object> config,
            Map<String, Object> stepCondition, String runMode, boolean continueOnError,
            int delaySeconds, int retryCount, Integer timeoutSeconds
    ) {
        public static ActionResponse from(AutomationAction a) {
            return new ActionResponse(a.getId(), a.getStepOrder(), a.getActionType(), a.getName(),
                    a.getConfig(), a.getStepCondition(), a.getRunMode(), a.isContinueOnError(),
                    a.getDelaySeconds(), a.getRetryCount(), a.getTimeoutSeconds());
        }
    }

    public record RuleResponse(
            Long id, String code, String name, String description, String category, int priority,
            Long companyId, Long branchId, String statusCode, String statusName, String triggerType,
            Map<String, Object> triggerConfig, Map<String, Object> conditionConfig,
            Map<String, Object> executionPolicy, boolean allowRecursion, Integer maxExecutions,
            long executionCount, List<ActionResponse> actions, Instant createdAt, Instant updatedAt, long version
    ) {
        public static RuleResponse from(AutomationRule r) {
            return new RuleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(),
                    r.getCategory(), r.getPriority(), r.getCompanyId(), r.getBranchId(),
                    r.getStatus().getCode(), r.getStatus().getName(), r.getTriggerType(),
                    r.getTriggerConfig(), r.getConditionConfig(), r.getExecutionPolicy(),
                    r.isAllowRecursion(), r.getMaxExecutions(), r.getExecutionCount(),
                    r.getActions().stream().map(ActionResponse::from).toList(),
                    r.getCreatedAt(), r.getUpdatedAt(), r.getVersion());
        }

        /** Lightweight row (no actions) for paginated listing. */
        public static RuleResponse row(AutomationRule r) {
            return new RuleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(),
                    r.getCategory(), r.getPriority(), r.getCompanyId(), r.getBranchId(),
                    r.getStatus().getCode(), r.getStatus().getName(), r.getTriggerType(),
                    Map.of(), Map.of(), Map.of(), r.isAllowRecursion(), r.getMaxExecutions(),
                    r.getExecutionCount(), List.of(), r.getCreatedAt(), r.getUpdatedAt(), r.getVersion());
        }
    }

    public record ExecutionResponse(
            Long id, String executionNumber, Long ruleId, String triggerType, String triggerRef,
            String entityType, Long entityId, String status, Map<String, Object> result, String error,
            Instant startedAt, Instant endedAt, Long durationMs, String executedByEmail
    ) {
        public static ExecutionResponse from(AutomationExecution e) {
            return new ExecutionResponse(e.getId(), e.getExecutionNumber(), e.getRule().getId(),
                    e.getTriggerType(), e.getTriggerRef(), e.getEntityType(), e.getEntityId(),
                    e.getStatus(), e.getResult(), e.getError(), e.getStartedAt(), e.getEndedAt(),
                    e.getDurationMs(), e.getExecutedByEmail());
        }
    }

    public record ExecutionLogResponse(
            Long id, int stepOrder, String actionType, String status, String message,
            Map<String, Object> detail, Instant occurredAt
    ) {
        public static ExecutionLogResponse from(AutomationExecutionLog l) {
            return new ExecutionLogResponse(l.getId(), l.getStepOrder(), l.getActionType(),
                    l.getStatus(), l.getMessage(), l.getDetail(), l.getOccurredAt());
        }
    }

    public record StatusResponse(Long id, String code, String name, boolean isActiveState,
                                 boolean isArchivedState, boolean isDefault, int displayOrder) {
        public static StatusResponse from(AutomationStatus s) {
            return new StatusResponse(s.getId(), s.getCode(), s.getName(), s.isActiveState(),
                    s.isArchivedState(), s.isDefault(), s.getDisplayOrder());
        }
    }

    public record TriggerDescriptorResponse(String type, String label, String category) {
    }

    public record ActionDescriptorResponse(String type, String label) {
    }

    public record RuleFilter(String search, String triggerType, String statusCode, String category) {
    }
}
