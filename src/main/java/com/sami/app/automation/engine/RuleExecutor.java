package com.sami.app.automation.engine;

import com.sami.app.automation.domain.AutomationAction;
import com.sami.app.automation.domain.AutomationExecution;
import com.sami.app.automation.domain.AutomationExecutionLog;
import com.sami.app.automation.domain.AutomationFailure;
import com.sami.app.automation.domain.AutomationRule;
import com.sami.app.automation.event.AutomationDomainEvent;
import com.sami.app.automation.repository.AutomationExecutionLogRepository;
import com.sami.app.automation.repository.AutomationExecutionRepository;
import com.sami.app.automation.repository.AutomationFailureRepository;
import com.sami.app.automation.repository.AutomationRuleRepository;
import com.sami.app.automation.spi.ActionContext;
import com.sami.app.automation.spi.ActionProvider;
import com.sami.app.automation.spi.ActionProviderRegistry;
import com.sami.app.automation.spi.ActionResult;
import com.sami.app.automation.spi.AutomationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Executes a single rule against a firing context in its own transaction, with
 * the safety guarantees the spec demands: loop / recursion / circular-workflow
 * protection (a per-thread active-rule stack plus a global depth cap), a
 * configurable execution policy (stop/continue on error, per-action retry) and a
 * full execution + step log. Kept separate from {@link AutomationEngine} so the
 * transactional boundary is honoured (external call → proxied).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecutor {

    private static final int MAX_DEPTH = 10;
    private static final ThreadLocal<Deque<Long>> ACTIVE_RULES = ThreadLocal.withInitial(ArrayDeque::new);

    private final AutomationRuleRepository ruleRepository;
    private final AutomationExecutionRepository executionRepository;
    private final AutomationExecutionLogRepository logRepository;
    private final AutomationFailureRepository failureRepository;
    private final ActionProviderRegistry actionRegistry;
    private final ConditionEvaluator conditionEvaluator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Runs one rule. Returns silently (no execution row) when guards or the
     * condition tree reject the firing; records a full execution otherwise.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Long ruleId, AutomationContext ctx) {
        AutomationRule rule = ruleRepository.findWithActionsById(ruleId).orElse(null);
        if (rule == null) {
            return;
        }

        Deque<Long> active = ACTIVE_RULES.get();
        boolean recursive = active.contains(ruleId);
        if ((recursive && !rule.isAllowRecursion()) || ctx.depth() >= MAX_DEPTH) {
            log.debug("Automation rule {} skipped: recursion/depth guard", rule.getCode());
            return;
        }
        if (rule.getMaxExecutions() != null && rule.getExecutionCount() >= rule.getMaxExecutions()) {
            return;
        }
        if (!conditionEvaluator.evaluate(rule.getConditionConfig(), ctx)) {
            return;
        }

        AutomationExecution execution = startExecution(rule, ctx);
        eventPublisher.publishEvent(new AutomationDomainEvent(
                "aex-" + execution.getExecutionNumber(), AutomationDomainEvent.EXECUTION_STARTED,
                rule.getId(), rule.getCode(), execution.getId(), Map.of(), execution.getStartedAt()));

        active.push(ruleId);
        Map<String, Object> policy = rule.getExecutionPolicy();
        boolean stopOnError = boolVal(policy.get("stopOnError"), true) && !boolVal(policy.get("ignoreErrors"), false);
        Map<String, Object> state = new HashMap<>();
        boolean failed = false;
        String failureMessage = null;
        try {
            for (AutomationAction action : rule.getActions()) {
                StepOutcome outcome = runStep(action, ctx, state, execution);
                if (!outcome.success()) {
                    if (!action.isContinueOnError() && stopOnError) {
                        failed = true;
                        failureMessage = outcome.message();
                        break;
                    }
                }
            }
        } finally {
            active.pop();
            if (active.isEmpty()) {
                ACTIVE_RULES.remove();
            }
        }

        finishExecution(rule, execution, state, failed, failureMessage, ctx);
    }

    private StepOutcome runStep(AutomationAction action, AutomationContext ctx,
                                Map<String, Object> state, AutomationExecution execution) {
        if (action.getStepCondition() != null && !conditionEvaluator.evaluate(action.getStepCondition(), ctx)) {
            writeLog(execution.getId(), action, "SKIPPED", "Step condition not met", null);
            return new StepOutcome(true, null);
        }
        ActionProvider provider = actionRegistry.find(action.getActionType()).orElse(null);
        if (provider == null) {
            writeLog(execution.getId(), action, "FAILED", "No provider for action type " + action.getActionType(), null);
            return new StepOutcome(false, "unavailable provider: " + action.getActionType());
        }

        int attempts = Math.max(0, action.getRetryCount()) + 1;
        ActionResult result = null;
        String error = null;
        for (int i = 0; i < attempts; i++) {
            try {
                result = provider.execute(new ActionContext(action.getConfig(), ctx, state));
                if (result.success()) {
                    break;
                }
                error = result.message();
            } catch (RuntimeException ex) {
                error = ex.getMessage();
                log.debug("Automation action {} attempt {} threw: {}", action.getActionType(), i + 1, error);
            }
        }

        boolean ok = result != null && result.success();
        if (ok && result.output() != null) {
            state.putAll(result.output());
        }
        writeLog(execution.getId(), action, ok ? "SUCCEEDED" : "FAILED",
                ok ? result.message() : error, ok ? result.output() : null);
        return new StepOutcome(ok, ok ? null : error);
    }

    private AutomationExecution startExecution(AutomationRule rule, AutomationContext ctx) {
        String number = "AUTO-" + String.format("%08d", executionRepository.nextNumber());
        return executionRepository.save(AutomationExecution.builder()
                .executionNumber(number)
                .rule(rule)
                .triggerType(ctx.triggerType())
                .triggerRef(ctx.entityType() != null ? ctx.entityType() + "#" + ctx.entityId() : null)
                .entityType(ctx.entityType())
                .entityId(ctx.entityId())
                .status(AutomationExecution.Status.RUNNING.name())
                .context(ctx.data())
                .startedAt(Instant.now())
                .executedBy(ctx.actorId())
                .build());
    }

    private void finishExecution(AutomationRule rule, AutomationExecution execution, Map<String, Object> state,
                                 boolean failed, String failureMessage, AutomationContext ctx) {
        Instant now = Instant.now();
        execution.setEndedAt(now);
        execution.setDurationMs(now.toEpochMilli() - execution.getStartedAt().toEpochMilli());
        execution.setStatus(failed ? AutomationExecution.Status.FAILED.name()
                : AutomationExecution.Status.SUCCEEDED.name());
        execution.setResult(state.isEmpty() ? null : new HashMap<>(state));
        execution.setError(failureMessage);
        executionRepository.save(execution);

        rule.setExecutionCount(rule.getExecutionCount() + 1);
        ruleRepository.save(rule);

        if (failed) {
            Map<String, Object> policy = rule.getExecutionPolicy();
            if (boolVal(policy.get("retryOnFailure"), false)) {
                long backoff = longVal(policy.get("retryDelaySeconds"), 60);
                failureRepository.save(AutomationFailure.builder()
                        .rule(rule)
                        .executionId(execution.getId())
                        .reason(failureMessage)
                        .payload(ctx.data())
                        .nextRetryAt(now.plusSeconds(backoff))
                        .build());
            }
        }

        eventPublisher.publishEvent(new AutomationDomainEvent(
                "aex-" + execution.getExecutionNumber(),
                failed ? AutomationDomainEvent.EXECUTION_FAILED : AutomationDomainEvent.EXECUTION_COMPLETED,
                rule.getId(), rule.getCode(), execution.getId(),
                Map.of("status", execution.getStatus()), now));
    }

    private void writeLog(Long executionId, AutomationAction action, String status,
                          String message, Map<String, Object> detail) {
        logRepository.save(AutomationExecutionLog.builder()
                .executionId(executionId)
                .stepOrder(action.getStepOrder())
                .actionType(action.getActionType())
                .status(status)
                .message(message == null ? null : message.substring(0, Math.min(message.length(), 1000)))
                .detail(detail == null || detail.isEmpty() ? null : detail)
                .build());
    }

    private static boolean boolVal(Object v, boolean fallback) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return fallback;
    }

    private static long longVal(Object v, long fallback) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private record StepOutcome(boolean success, String message) {
    }
}
