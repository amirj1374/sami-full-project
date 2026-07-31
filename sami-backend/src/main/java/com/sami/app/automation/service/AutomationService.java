package com.sami.app.automation.service;

import com.sami.app.automation.domain.AutomationAction;
import com.sami.app.automation.domain.AutomationExecution;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return statusRepository.findAllByOrderByDisplayOrderAsc().stream().map(StatusResponse::from).toList();
    }

    public List<TriggerDescriptorResponse> triggers() {
        return triggerRegistry.all().stream()
                .map(t -> new TriggerDescriptorResponse(t.type(), t.label(), t.category())).toList();
    }

    public List<ActionDescriptorResponse> actions() {
        return actionRegistry.all().stream()
                .map(a -> new ActionDescriptorResponse(a.type(), a.label())).toList();
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
                saved.getId(), saved.getCode(), null, Map.of(), Instant.now()));
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
                saved.getId(), saved.getCode(), null, Map.of(), Instant.now()));
        return RuleResponse.from(saved);
    }

    @Transactional
    public RuleResponse changeStatus(Long id, String statusCode, Long expectedVersion) {
        AutomationRule rule = loadRule(id);
        checkVersion(rule, expectedVersion);
        AutomationStatus status = statusRepository.findByCode(statusCode)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown status: " + statusCode));
        String from = rule.getStatus().getCode();
        rule.setStatus(status);
        AutomationRule saved = ruleRepository.save(rule);

        audit.record("RULE", id, "STATUS_CHANGED",
                Map.of("status", from), Map.of("status", statusCode));
        eventPublisher.publishEvent(new AutomationDomainEvent(
                "rule-" + id, status.isActiveState() ? AutomationDomainEvent.RULE_ACTIVATED
                        : AutomationDomainEvent.RULE_DISABLED,
                id, saved.getCode(), null, Map.of("status", statusCode), Instant.now()));
        return RuleResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        AutomationRule rule = loadRule(id);
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

    // ---- Helpers ------------------------------------------------------------

    private void apply(AutomationRule rule, RuleRequest request) {
        AutomationStatus status = statusRepository.findByCode(request.statusCode())
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown status: " + request.statusCode()));
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
                    .runMode(ar.runMode() != null ? ar.runMode() : "SEQUENTIAL")
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
}
