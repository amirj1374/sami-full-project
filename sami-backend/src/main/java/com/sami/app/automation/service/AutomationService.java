package com.sami.app.automation.service;

import com.sami.app.automation.domain.AutomationAction;
import com.sami.app.automation.domain.AutomationExecution;
import com.sami.app.automation.domain.AutomationFailure;
import com.sami.app.automation.domain.AutomationRule;
import com.sami.app.automation.domain.AutomationStatus;
import com.sami.app.automation.dto.AutomationDtos.ActionDescriptorResponse;
import com.sami.app.automation.dto.AutomationDtos.ActionRequest;
import com.sami.app.automation.dto.AutomationDtos.ExecutionLogResponse;
import com.sami.app.automation.dto.AutomationDtos.ExecutionResponse;
import com.sami.app.automation.dto.AutomationDtos.RuleFilter;
import com.sami.app.automation.dto.AutomationDtos.RuleRequest;
import com.sami.app.automation.dto.AutomationDtos.RuleResponse;
import com.sami.app.automation.dto.AutomationDtos.RunRequest;
import com.sami.app.automation.dto.AutomationDtos.StatusResponse;
import com.sami.app.automation.dto.AutomationDtos.TriggerDescriptorResponse;
import com.sami.app.automation.engine.AutomationEngine;
import com.sami.app.automation.event.AutomationDomainEvent;
import com.sami.app.automation.repository.AutomationExecutionLogRepository;
import com.sami.app.automation.repository.AutomationExecutionRepository;
import com.sami.app.automation.repository.AutomationRuleRepository;
import com.sami.app.automation.repository.AutomationStatusRepository;
import com.sami.app.automation.repository.AutomationFailureRepository;
import com.sami.app.automation.dto.AutomationDtos.FailureResponse;
import com.sami.app.automation.dto.AutomationDtos.MonitoringResponse;
import com.sami.app.automation.spi.ActionProvider;
import com.sami.app.automation.spi.ActionProviderRegistry;
import com.sami.app.automation.spi.AutomationContext;
import com.sami.app.automation.spi.TriggerRegistry;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;

/**
 * Application service for automation rules: configuration CRUD (with validation
 * against the SPI registries — no unknown trigger/action can be saved), lifecycle
 * status changes, manual execution, and read models for executions/logs/catalogs.
 * All orchestration goes through the engine; no business logic lives here.
 */
@Service
@RequiredArgsConstructor
public class AutomationService {

    private final AutomationRuleRepository ruleRepository;
    private final AutomationStatusRepository statusRepository;
    private final AutomationExecutionRepository executionRepository;
    private final AutomationExecutionLogRepository executionLogRepository;
    private final ActionProviderRegistry actionRegistry;
    private final TriggerRegistry triggerRegistry;
    private final AutomationEngine engine;
    private final AutomationAuditService audit;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantContext tenantContext;
    private final AutomationFailureRepository failureRepository;

    // ---- Read ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<RuleResponse> list(RuleFilter filter, Pageable pageable) {
        return ruleRepository.findAll(toSpecification(filter, tenantContext.requireTenantId()), pageable).map(RuleResponse::row);
    }

    @Transactional(readOnly = true)
    public RuleResponse get(Long id) {
        return RuleResponse.from(loadRule(id));
    }

    @Transactional(readOnly = true)
    public Page<ExecutionResponse> executions(Long ruleId, Pageable pageable) {
        AutomationRule rule = loadRule(ruleId);
        return executionRepository.findByRuleIdAndTenantIdOrderByStartedAtDesc(rule.getId(), rule.getTenantId(), pageable)
                .map(ExecutionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ExecutionLogResponse> executionLogs(Long executionId) {
        executionRepository.findByIdAndTenantId(executionId, tenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Automation execution not found: " + executionId));
        return executionLogRepository.findByExecutionIdOrderByStepOrderAsc(executionId)
                .stream().map(ExecutionLogResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StatusResponse> statuses() {
        Long tenantId = tenantContext.requireTenantId();
        return new ArrayList<>(statusRepository.findVisible(tenantId).stream()
                .collect(java.util.stream.Collectors.toMap(AutomationStatus::getCode, status -> status,
                        (preferred, ignored) -> preferred, LinkedHashMap::new)).values()).stream()
                .sorted(Comparator.comparingInt(AutomationStatus::getDisplayOrder))
                .map(StatusResponse::from).toList();
    }

    public List<TriggerDescriptorResponse> triggers() {
        return triggerRegistry.all().stream()
                .map(t -> new TriggerDescriptorResponse(t.type(), t.label(), t.category())).toList();
    }

    public List<ActionDescriptorResponse> actions() {
        return actionRegistry.all().stream()
                .map(a -> new ActionDescriptorResponse(a.type(), a.label())).toList();
    }

    @Transactional(readOnly = true)
    public MonitoringResponse monitoring() {
        Long tenantId = tenantContext.requireTenantId();
        return new MonitoringResponse(
                executionRepository.countByTenantIdAndStatus(tenantId, AutomationExecution.Status.RUNNING.name()),
                executionRepository.countByTenantIdAndStatus(tenantId, AutomationExecution.Status.SUCCEEDED.name()),
                executionRepository.countByTenantIdAndStatus(tenantId, AutomationExecution.Status.FAILED.name()),
                failureRepository.countByTenantIdAndResolved(tenantId, false),
                ruleRepository.countByTenantId(tenantId), Instant.now());
    }

    @Transactional(readOnly = true)
    public Page<FailureResponse> failures(boolean resolved, Pageable pageable) {
        return failureRepository.findByTenantIdAndResolvedOrderByCreatedAtDesc(
                tenantContext.requireTenantId(), resolved, pageable).map(FailureResponse::from);
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> exportConfiguration() {
        return ruleRepository.findTop10000ByTenantIdOrderByPriorityAsc(tenantContext.requireTenantId())
                .stream().map(RuleResponse::from).toList();
    }

    @Transactional
    public List<RuleResponse> importConfiguration(List<RuleRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "At least one automation rule is required");
        }
        if (requests.size() > 500) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "A single import may contain at most 500 rules");
        }
        List<RuleResponse> imported = new ArrayList<>();
        for (RuleRequest request : requests) {
            imported.add(create(request));
        }
        return imported;
    }

    @Transactional(readOnly = true)
    public byte[] executionReportCsv() {
        Long tenantId = tenantContext.requireTenantId();
        StringBuilder csv = new StringBuilder("\uFEFFExecution,Rule,Trigger,Status,Started,Ended,DurationMs,Error\r\n");
        executionRepository.findByTenantIdOrderByStartedAtDesc(tenantId,
                org.springframework.data.domain.PageRequest.of(0, 10_000)).forEach(execution -> csv
                .append(csv(execution.getExecutionNumber())).append(',')
                .append(csv(execution.getRule().getCode())).append(',')
                .append(csv(execution.getTriggerType())).append(',')
                .append(csv(execution.getStatus())).append(',')
                .append(execution.getStartedAt()).append(',')
                .append(execution.getEndedAt() == null ? "" : execution.getEndedAt()).append(',')
                .append(execution.getDurationMs() == null ? "" : execution.getDurationMs()).append(',')
                .append(csv(execution.getError())).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ---- Write --------------------------------------------------------------

    @Transactional
    public RuleResponse create(RuleRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        if (ruleRepository.existsByTenantIdAndCode(tenantId, request.code())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Automation code already exists: " + request.code());
        }
        AutomationRule rule = new AutomationRule();
        rule.setTenantId(tenantId);
        // @Builder.Default leaves collections null under the no-args constructor.
        rule.setActions(new ArrayList<>());
        rule.setTriggerConfig(new HashMap<>());
        rule.setConditionConfig(new HashMap<>());
        rule.setExecutionPolicy(new HashMap<>());
        rule.setCode(request.code());
        apply(rule, request);
        rule.setCreatedBy(CurrentActor.id());
        rule.setCreatedByEmail(CurrentActor.email());
        AutomationRule saved = ruleRepository.save(rule);

        audit.record("RULE", saved.getId(), "CREATED", null, snapshot(saved));
        eventPublisher.publishEvent(new AutomationDomainEvent(
                "rule-" + saved.getId(), AutomationDomainEvent.RULE_CREATED,
                saved.getTenantId(), saved.getId(), saved.getCode(), null, Map.of(), Instant.now()));
        return RuleResponse.from(saved);
    }

    @Transactional
    public RuleResponse update(Long id, RuleRequest request) {
        AutomationRule rule = loadRule(id);
        checkVersion(rule, request.expectedVersion());
        Map<String, Object> before = snapshot(rule);
        // Code is immutable after creation.
        apply(rule, request);
        rule.getActions().clear();
        ruleRepository.flush();
        applyActions(rule, request.actions());
        AutomationRule saved = ruleRepository.save(rule);

        audit.record("RULE", saved.getId(), "UPDATED", before, snapshot(saved));
        eventPublisher.publishEvent(new AutomationDomainEvent(
                "rule-" + saved.getId(), AutomationDomainEvent.RULE_UPDATED,
                saved.getTenantId(), saved.getId(), saved.getCode(), null, Map.of(), Instant.now()));
        return RuleResponse.from(saved);
    }

    @Transactional
    public RuleResponse changeStatus(Long id, String statusCode, Long expectedVersion) {
        AutomationRule rule = loadRule(id);
        checkVersion(rule, expectedVersion);
        AutomationStatus status = resolveStatus(statusCode);
        String from = rule.getStatus().getCode();
        rule.setStatus(status);
        AutomationRule saved = ruleRepository.save(rule);

        audit.record("RULE", id, "STATUS_CHANGED",
                Map.of("status", from), Map.of("status", statusCode));
        eventPublisher.publishEvent(new AutomationDomainEvent(
                "rule-" + id, status.isActiveState() ? AutomationDomainEvent.RULE_ACTIVATED
                        : AutomationDomainEvent.RULE_DISABLED,
                saved.getTenantId(), id, saved.getCode(), null, Map.of("status", statusCode), Instant.now()));
        return RuleResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        AutomationRule rule = loadRule(id);
        if (executionRepository.existsByRuleIdAndTenantId(rule.getId(), rule.getTenantId())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Automation rules with execution history cannot be deleted; archive the rule instead");
        }
        audit.record("RULE", id, "DELETED", snapshot(rule), null);
        ruleRepository.delete(rule);
    }

    /** Manual execution: build a context from the request and run the rule directly. */
    @Transactional
    public void run(Long id, RunRequest request) {
        AutomationRule rule = loadRule(id);
        AutomationContext ctx = new AutomationContext(
                rule.getTriggerType(),
                request != null ? request.entityType() : null,
                request != null ? request.entityType() : null,
                request != null ? request.entityId() : null,
                request != null && request.data() != null ? request.data() : Map.of(),
                rule.getTenantId(), rule.getCompanyId(), rule.getBranchId(), CurrentActor.id(), Instant.now(), 0);
        engine.executeRule(id, ctx);
    }

    @Transactional
    public FailureResponse retryFailure(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        AutomationFailure failure = loadFailure(id, tenantId);
        retry(failure, tenantId);
        return FailureResponse.from(failureRepository.save(failure));
    }

    @Transactional
    public FailureResponse resolveFailure(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        AutomationFailure failure = loadFailure(id, tenantId);
        resolve(failure);
        audit.record("FAILURE", failure.getId(), "RESOLVED", null, failureSnapshot(failure));
        eventPublisher.publishEvent(new AutomationDomainEvent(
                "afail-" + failure.getId(), AutomationDomainEvent.FAILURE_RESOLVED,
                tenantId, failure.getRule().getId(), failure.getRule().getCode(),
                failure.getExecutionId(), Map.of(), Instant.now()));
        return FailureResponse.from(failureRepository.save(failure));
    }

    /** Tenant-explicit entry point for the shared scheduler; it never reads HTTP context. */
    @Transactional
    public int retryDueFailures(Long tenantId, int requestedLimit) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Trusted tenant scope is required");
        }
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        List<AutomationFailure> due = failureRepository
                .findByTenantIdAndResolvedFalseAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                        tenantId, Instant.now(), PageRequest.of(0, limit));
        int processed = 0;
        for (AutomationFailure failure : due) {
            retry(failure, tenantId);
            failureRepository.save(failure);
            processed++;
        }
        return processed;
    }

    // ---- Helpers ------------------------------------------------------------

    private void apply(AutomationRule rule, RuleRequest request) {
        AutomationStatus status = resolveStatus(request.statusCode());
        if (request.triggerType().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "triggerType is required");
        }
        rule.setName(request.name());
        rule.setDescription(request.description());
        rule.setCategory(request.category());
        rule.setPriority(request.priority() != null ? request.priority() : 100);
        rule.setCompanyId(request.companyId());
        rule.setBranchId(request.branchId());
        rule.setStatus(status);
        rule.setTriggerType(request.triggerType());
        rule.setTriggerConfig(nullSafe(request.triggerConfig()));
        rule.setConditionConfig(nullSafe(request.conditionConfig()));
        rule.setExecutionPolicy(nullSafe(request.executionPolicy()));
        rule.setAllowRecursion(request.allowRecursion());
        rule.setMaxExecutions(request.maxExecutions());
        if (rule.getId() == null) {
            applyActions(rule, request.actions());
        }
    }

    private void applyActions(AutomationRule rule, List<ActionRequest> requests) {
        if (requests == null) {
            return;
        }
        List<AutomationAction> actions = new ArrayList<>();
        int order = 0;
        for (ActionRequest ar : requests) {
            ActionProvider provider = actionRegistry.find(ar.actionType())
                    .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                            "Unknown action type: " + ar.actionType()));
            Map<String, Object> config = nullSafe(ar.config());
            String error = provider.validate(config);
            if (error != null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, error);
            }
            actions.add(AutomationAction.builder()
                    .rule(rule)
                    .stepOrder(ar.stepOrder() > 0 ? ar.stepOrder() : ++order)
                    .actionType(ar.actionType())
                    .name(ar.name())
                    .config(config)
                    .stepCondition(ar.stepCondition())
                    .runMode(ar.runMode() == null || "SYNC".equals(ar.runMode())
                            ? "SEQUENTIAL" : ar.runMode())
                    .continueOnError(ar.continueOnError())
                    .delaySeconds(ar.delaySeconds())
                    .retryCount(ar.retryCount())
                    .timeoutSeconds(ar.timeoutSeconds())
                    .build());
        }
        rule.getActions().addAll(actions);
    }

    private AutomationRule loadRule(Long id) {
        return ruleRepository.findWithActionsByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found: " + id));
    }

    private AutomationStatus resolveStatus(String code) {
        return statusRepository.findVisibleByCode(tenantContext.requireTenantId(), code).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown status: " + code));
    }

    private AutomationFailure loadFailure(Long id, Long tenantId) {
        return failureRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation failure not found: " + id));
    }

    private AutomationExecution.Status retry(AutomationFailure failure, Long tenantId) {
        if (failure.isResolved()) {
            return AutomationExecution.Status.SKIPPED;
        }
        AutomationRule rule = failure.getRule();
        Map<String, Object> payload = failure.getPayload() == null ? Map.of() : failure.getPayload();
        AutomationExecution.Status outcome = engine.executeRule(rule.getId(), new AutomationContext(
                rule.getTriggerType(), null, null, null, payload, tenantId,
                rule.getCompanyId(), rule.getBranchId(), CurrentActor.id(), Instant.now(), 0));
        failure.setRetryCount(failure.getRetryCount() + 1);
        if (outcome != AutomationExecution.Status.SKIPPED) {
            resolve(failure);
        } else {
            failure.setNextRetryAt(Instant.now().plusSeconds(retryDelay(rule)));
        }
        eventPublisher.publishEvent(new AutomationDomainEvent(
                "afail-" + failure.getId() + "-retry-" + failure.getRetryCount(),
                AutomationDomainEvent.FAILURE_RETRIED, tenantId, rule.getId(), rule.getCode(),
                failure.getExecutionId(), Map.of("outcome", outcome.name(),
                        "retryCount", failure.getRetryCount()), Instant.now()));
        audit.recordForTenant(tenantId, "FAILURE", failure.getId(), "RETRIED", null,
                failureSnapshot(failure));
        return outcome;
    }

    private void resolve(AutomationFailure failure) {
        failure.setResolved(true);
        failure.setResolvedAt(Instant.now());
        failure.setNextRetryAt(null);
    }

    private long retryDelay(AutomationRule rule) {
        Object configured = rule.getExecutionPolicy().get("retryDelaySeconds");
        if (configured instanceof Number number) {
            return Math.max(1, number.longValue());
        }
        return 60;
    }

    private Map<String, Object> failureSnapshot(AutomationFailure failure) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("executionId", failure.getExecutionId());
        snapshot.put("retryCount", failure.getRetryCount());
        snapshot.put("resolved", failure.isResolved());
        return snapshot;
    }

    private void checkVersion(AutomationRule rule, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(rule.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Automation was modified by someone else; reload and retry");
        }
    }

    private Specification<AutomationRule> toSpecification(RuleFilter filter, Long tenantId) {
        return Specification.allOf(
                (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId),
                searchSpec(filter == null ? null : filter.search()),
                equalSpec("triggerType", filter == null ? null : filter.triggerType()),
                statusSpec(filter == null ? null : filter.statusCode()),
                equalSpec("category", filter == null ? null : filter.category()));
    }

    private Specification<AutomationRule> searchSpec(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), like),
                cb.like(cb.lower(root.get("name")), like));
    }

    private Specification<AutomationRule> equalSpec(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    private Specification<AutomationRule> statusSpec(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status").get("code"), statusCode);
    }

    private Map<String, Object> nullSafe(Map<String, Object> map) {
        return map == null ? new HashMap<>() : new HashMap<>(map);
    }

    private Map<String, Object> snapshot(AutomationRule rule) {
        Map<String, Object> snap = new HashMap<>();
        snap.put("code", rule.getCode());
        snap.put("name", rule.getName());
        snap.put("triggerType", rule.getTriggerType());
        snap.put("status", rule.getStatus() != null ? rule.getStatus().getCode() : null);
        snap.put("priority", rule.getPriority());
        snap.put("actionCount", rule.getActions().size());
        return snap;
    }

    private String csv(String value) {
        return value == null ? "" : '"' + value.replace("\"", "\"\"") + '"';
    }
}
