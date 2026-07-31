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
import com.sami.app.common.tenancy.TenantContext;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private final TenantContext tenantContext;
    private final PlatformTransactionManager transactionManager;
    private final ExecutorService actionExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Runs one rule. Returns silently (no execution row) when guards or the
     * condition tree reject the firing; records a full execution otherwise.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AutomationExecution.Status execute(Long ruleId, AutomationContext ctx) {
        AutomationRule rule = ruleRepository.findWithActionsByIdAndTenantId(ruleId, ctx.tenantId()).orElse(null);
        if (rule == null) {
            return AutomationExecution.Status.SKIPPED;
        }

        Deque<Long> active = ACTIVE_RULES.get();
        boolean recursive = active.contains(ruleId);
        if ((recursive && !rule.isAllowRecursion()) || ctx.depth() >= MAX_DEPTH) {
            log.debug("Automation rule {} skipped: recursion/depth guard", rule.getCode());
            return AutomationExecution.Status.SKIPPED;
        }
        if (rule.getMaxExecutions() != null && rule.getExecutionCount() >= rule.getMaxExecutions()) {
            return AutomationExecution.Status.SKIPPED;
        }
        if (!conditionEvaluator.evaluate(rule.getConditionConfig(), ctx)) {
            return AutomationExecution.Status.SKIPPED;
        }

        AutomationExecution execution = startExecution(rule, ctx);
        eventPublisher.publishEvent(new AutomationDomainEvent(
                "aex-" + execution.getExecutionNumber(), AutomationDomainEvent.EXECUTION_STARTED,
                ctx.tenantId(), rule.getId(), rule.getCode(), execution.getId(), Map.of(), execution.getStartedAt()));

        active.push(ruleId);
        Map<String, Object> policy = rule.getExecutionPolicy();
        boolean stopOnError = boolVal(policy.get("stopOnError"), true) && !boolVal(policy.get("ignoreErrors"), false);
        Map<String, Object> state = new HashMap<>();
        boolean failed = false;
        String failureMessage = null;
        try {
            List<AutomationAction> steps = rule.getActions();
            for (int index = 0; index < steps.size();) {
                AutomationAction action = steps.get(index);
                if ("PARALLEL".equalsIgnoreCase(action.getRunMode())) {
                    int end = index + 1;
                    while (end < steps.size()
                            && "PARALLEL".equalsIgnoreCase(steps.get(end).getRunMode())) {
                        end++;
                    }
                    List<AutomationAction> group = steps.subList(index, end);
                    List<StepOutcome> outcomes = runParallel(group, ctx, state, execution);
                    for (int offset = 0; offset < group.size(); offset++) {
                        StepOutcome outcome = outcomes.get(offset);
                        if (!outcome.success() && !group.get(offset).isContinueOnError() && stopOnError) {
                            failed = true;
                            failureMessage = outcome.message();
                            break;
                        }
                    }
                    index = end;
                } else {
                    StepOutcome outcome = runStep(action, ctx, state, execution);
                    if (!outcome.success() && !action.isContinueOnError() && stopOnError) {
                        failed = true;
                        failureMessage = outcome.message();
                    }
                    index++;
                }
                if (failed) {
                    break;
                }
            }
        } finally {
            active.pop();
            if (active.isEmpty()) {
                ACTIVE_RULES.remove();
            }
        }

        finishExecution(rule, execution, state, failed, failureMessage, ctx);
        return failed ? AutomationExecution.Status.FAILED : AutomationExecution.Status.SUCCEEDED;
    }

    private StepOutcome runStep(AutomationAction action, AutomationContext ctx,
                                Map<String, Object> state, AutomationExecution execution) {
        StepOutcome outcome;
        if (action.getTimeoutSeconds() == null) {
            outcome = evaluateStep(action, ctx, new HashMap<>(state), false);
        } else {
            long submittedAt = System.nanoTime();
            Future<StepOutcome> future = submit(action, ctx, state);
            outcome = await(action, future, submittedAt);
        }
        recordOutcome(execution, action, state, outcome);
        return outcome;
    }

    private List<StepOutcome> runParallel(List<AutomationAction> actions, AutomationContext ctx,
                                          Map<String, Object> state, AutomationExecution execution) {
        Map<String, Object> snapshot = new HashMap<>(state);
        long submittedAt = System.nanoTime();
        List<Future<StepOutcome>> futures = actions.stream()
                .map(action -> submit(action, ctx, snapshot))
                .toList();
        List<StepOutcome> outcomes = new ArrayList<>(actions.size());
        for (int index = 0; index < actions.size(); index++) {
            AutomationAction action = actions.get(index);
            StepOutcome outcome = await(action, futures.get(index), submittedAt);
            recordOutcome(execution, action, state, outcome);
            outcomes.add(outcome);
        }
        return outcomes;
    }

    private Future<StepOutcome> submit(AutomationAction action, AutomationContext ctx,
                                       Map<String, Object> state) {
        SecurityContext capturedSecurity = SecurityContextHolder.getContext();
        return actionExecutor.submit(() -> {
            SecurityContext previousSecurity = SecurityContextHolder.getContext();
            SecurityContextHolder.setContext(capturedSecurity);
            try {
                return tenantContext.callAsTenant(ctx.tenantId(),
                        () -> evaluateStep(action, ctx, new HashMap<>(state), true));
            } finally {
                SecurityContextHolder.setContext(previousSecurity);
            }
        });
    }

    private StepOutcome evaluateStep(AutomationAction action, AutomationContext ctx,
                                     Map<String, Object> state, boolean isolatedTransaction) {
        if (action.getStepCondition() != null && !conditionEvaluator.evaluate(action.getStepCondition(), ctx)) {
            return new StepOutcome(true, "Step condition not met", Map.of(), true);
        }
        ActionProvider provider = actionRegistry.find(action.getActionType()).orElse(null);
        if (provider == null) {
            return new StepOutcome(false, "No provider for action type " + action.getActionType(), Map.of(), false);
        }
        if (action.getDelaySeconds() > 0) {
            try {
                TimeUnit.SECONDS.sleep(action.getDelaySeconds());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return new StepOutcome(false, "Action interrupted during delay", Map.of(), false);
            }
        }

        int attempts = Math.max(0, action.getRetryCount()) + 1;
        ActionResult result = null;
        String error = null;
        for (int i = 0; i < attempts; i++) {
            try {
                ActionContext actionContext = new ActionContext(action.getConfig(), ctx, state);
                result = isolatedTransaction
                        ? executeInNewTransaction(provider, actionContext)
                        : provider.execute(actionContext);
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
        return new StepOutcome(ok, ok ? result.message() : error,
                ok && result.output() != null ? result.output() : Map.of(), false);
    }

    private ActionResult executeInNewTransaction(ActionProvider provider, ActionContext context) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> provider.execute(context));
    }

    private StepOutcome await(AutomationAction action, Future<StepOutcome> future, long submittedAt) {
        try {
            if (action.getTimeoutSeconds() == null) {
                return future.get();
            }
            if (future.isDone()) {
                return future.get();
            }
            long timeoutNanos = TimeUnit.SECONDS.toNanos(action.getTimeoutSeconds());
            long remaining = timeoutNanos - (System.nanoTime() - submittedAt);
            if (remaining <= 0) {
                throw new TimeoutException();
            }
            return future.get(remaining, TimeUnit.NANOSECONDS);
        } catch (TimeoutException timeout) {
            future.cancel(true);
            return new StepOutcome(false,
                    "Action timed out after " + action.getTimeoutSeconds() + " seconds", Map.of(), false);
        } catch (InterruptedException interrupted) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            return new StepOutcome(false, "Action execution interrupted", Map.of(), false);
        } catch (Exception failure) {
            Throwable cause = failure.getCause() == null ? failure : failure.getCause();
            return new StepOutcome(false, cause.getMessage(), Map.of(), false);
        }
    }

    private void recordOutcome(AutomationExecution execution, AutomationAction action,
                               Map<String, Object> state, StepOutcome outcome) {
        if (outcome.success() && !outcome.skipped()) {
            state.putAll(outcome.output());
        }
        writeLog(execution.getId(), action,
                outcome.skipped() ? "SKIPPED" : outcome.success() ? "SUCCEEDED" : "FAILED",
                outcome.message(), outcome.output());
    }

    private AutomationExecution startExecution(AutomationRule rule, AutomationContext ctx) {
        String number = "AUTO-" + String.format("%08d", executionRepository.nextNumber());
        return executionRepository.save(AutomationExecution.builder()
                .tenantId(ctx.tenantId())
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
                        .tenantId(ctx.tenantId())
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
                ctx.tenantId(), rule.getId(), rule.getCode(), execution.getId(),
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

    @PreDestroy
    void shutdown() {
        actionExecutor.shutdownNow();
    }

    private record StepOutcome(boolean success, String message, Map<String, Object> output, boolean skipped) {
    }
}
