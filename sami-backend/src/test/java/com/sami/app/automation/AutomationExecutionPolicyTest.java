package com.sami.app.automation;

import com.sami.app.automation.domain.AutomationAction;
import com.sami.app.automation.domain.AutomationExecution;
import com.sami.app.automation.domain.AutomationRule;
import com.sami.app.automation.engine.ConditionEvaluator;
import com.sami.app.automation.engine.RuleExecutor;
import com.sami.app.automation.repository.AutomationExecutionLogRepository;
import com.sami.app.automation.repository.AutomationExecutionRepository;
import com.sami.app.automation.repository.AutomationFailureRepository;
import com.sami.app.automation.repository.AutomationRuleRepository;
import com.sami.app.automation.spi.ActionProvider;
import com.sami.app.automation.spi.ActionProviderRegistry;
import com.sami.app.automation.spi.ActionResult;
import com.sami.app.automation.spi.AutomationContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutomationExecutionPolicyTest {

    @Test
    void contiguousParallelActionsExecuteConcurrently() {
        AutomationRuleRepository rules = mock(AutomationRuleRepository.class);
        AutomationExecutionRepository executions = mock(AutomationExecutionRepository.class);
        AutomationExecutionLogRepository logs = mock(AutomationExecutionLogRepository.class);
        AutomationFailureRepository failures = mock(AutomationFailureRepository.class);
        ActionProviderRegistry providers = mock(ActionProviderRegistry.class);
        ConditionEvaluator conditions = mock(ConditionEvaluator.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(mock(TransactionStatus.class));
        RuleExecutor executor = new RuleExecutor(
                rules, executions, logs, failures, providers, conditions, events,
                new com.sami.app.common.tenancy.TenantContext(), transactions);

        AutomationRule rule = ruleWithParallelActions();
        when(rules.findWithActionsByIdAndTenantId(4L, 31L)).thenReturn(Optional.of(rule));
        when(executions.nextNumber()).thenReturn(1L);
        when(executions.save(any())).thenAnswer(invocation -> {
            AutomationExecution execution = invocation.getArgument(0);
            if (execution.getId() == null) ReflectionTestUtils.setField(execution, "id", 22L);
            return execution;
        });
        when(conditions.evaluate(any(), any())).thenReturn(true);

        CountDownLatch bothStarted = new CountDownLatch(2);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        ActionProvider provider = new ActionProvider() {
            public String type() { return "probe"; }
            public String label() { return "Probe"; }
            public ActionResult execute(com.sami.app.automation.spi.ActionContext context) {
                int current = active.incrementAndGet();
                peak.accumulateAndGet(current, Math::max);
                bothStarted.countDown();
                try {
                    assertTrue(bothStarted.await(2, TimeUnit.SECONDS));
                    return ActionResult.ok();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return ActionResult.fail("interrupted");
                } finally {
                    active.decrementAndGet();
                }
            }
        };
        when(providers.find("probe")).thenReturn(Optional.of(provider));

        AutomationExecution.Status result = executor.execute(4L, new AutomationContext(
                "manual", null, null, null, Map.of(), 31L,
                null, null, 2L, Instant.now(), 0));

        assertEquals(AutomationExecution.Status.SUCCEEDED, result);
        assertEquals(2, peak.get());
        verify(logs, times(2)).save(any());
    }

    private AutomationRule ruleWithParallelActions() {
        AutomationRule rule = new AutomationRule();
        ReflectionTestUtils.setField(rule, "id", 4L);
        rule.setTenantId(31L);
        rule.setCode("parallel-test");
        rule.setTriggerType("manual");
        rule.setConditionConfig(Map.of());
        rule.setExecutionPolicy(Map.of());
        AutomationAction first = action(rule, 1);
        AutomationAction second = action(rule, 2);
        rule.setActions(List.of(first, second));
        return rule;
    }

    private AutomationAction action(AutomationRule rule, int order) {
        return AutomationAction.builder()
                .rule(rule)
                .stepOrder(order)
                .actionType("probe")
                .config(Map.of())
                .runMode("PARALLEL")
                .timeoutSeconds(3)
                .build();
    }
}
