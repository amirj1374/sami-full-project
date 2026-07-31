package com.sami.app.automation;

import com.sami.app.automation.domain.AutomationExecution;
import com.sami.app.automation.domain.AutomationFailure;
import com.sami.app.automation.domain.AutomationRule;
import com.sami.app.automation.engine.AutomationEngine;
import com.sami.app.automation.event.AutomationDomainEvent;
import com.sami.app.automation.provider.AutomationFailureRetryJobHandler;
import com.sami.app.automation.repository.AutomationExecutionLogRepository;
import com.sami.app.automation.repository.AutomationExecutionRepository;
import com.sami.app.automation.repository.AutomationFailureRepository;
import com.sami.app.automation.repository.AutomationRuleRepository;
import com.sami.app.automation.repository.AutomationStatusRepository;
import com.sami.app.automation.service.AutomationAuditService;
import com.sami.app.automation.service.AutomationService;
import com.sami.app.automation.spi.ActionProviderRegistry;
import com.sami.app.automation.spi.TriggerRegistry;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.tenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomationFailureOperationsTest {

    @Mock AutomationRuleRepository rules;
    @Mock AutomationStatusRepository statuses;
    @Mock AutomationExecutionRepository executions;
    @Mock AutomationExecutionLogRepository logs;
    @Mock ActionProviderRegistry actions;
    @Mock TriggerRegistry triggers;
    @Mock AutomationEngine engine;
    @Mock AutomationAuditService audit;
    @Mock ApplicationEventPublisher events;
    @Mock TenantContext tenantContext;
    @Mock AutomationFailureRepository failures;
    @InjectMocks AutomationService service;

    @Test
    void retryUsesTrustedTenantAndPublishesTenantScopedEvent() {
        AutomationFailure failure = failure(8L, 42L);
        when(tenantContext.requireTenantId()).thenReturn(42L);
        when(failures.findByIdAndTenantId(8L, 42L)).thenReturn(Optional.of(failure));
        when(failures.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(engine.executeRule(eq(6L), any())).thenReturn(AutomationExecution.Status.SUCCEEDED);

        var response = service.retryFailure(8L);

        assertTrue(response.resolved());
        assertEquals(1, response.retryCount());
        verify(engine).executeRule(eq(6L), argThat(context -> context.tenantId().equals(42L)));
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertEquals(42L, ((AutomationDomainEvent) event.getValue()).tenantId());
    }

    @Test
    void schedulerRetryUsesExplicitTenantWithoutRequestContext() {
        AutomationFailure failure = failure(9L, 73L);
        when(failures.findByTenantIdAndResolvedFalseAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                eq(73L), any(Instant.class), any(Pageable.class))).thenReturn(List.of(failure));
        when(engine.executeRule(eq(6L), any())).thenReturn(AutomationExecution.Status.SUCCEEDED);

        int processed = service.retryDueFailures(73L, 10);

        assertEquals(1, processed);
        verifyNoInteractions(tenantContext);
        verify(engine).executeRule(eq(6L), argThat(context -> context.tenantId().equals(73L)));
    }

    @Test
    void retryHandlerFailsClosedWithoutTenant() {
        AutomationFailureRetryJobHandler handler = new AutomationFailureRetryJobHandler(service, new TenantContext());
        var result = handler.execute(new JobContext("automation-retry", Map.of(), null,
                Instant.now(), false, null));

        assertFalse(result.success());
        verifyNoInteractions(failures, engine);
    }

    @Test
    void executionReportIsTenantScopedUtf8WithBom() {
        AutomationRule rule = new AutomationRule();
        ReflectionTestUtils.setField(rule, "id", 6L);
        rule.setCode("customer-report");
        AutomationExecution execution = AutomationExecution.builder()
                .tenantId(42L)
                .executionNumber("AUTO-00000001")
                .rule(rule)
                .triggerType("manual")
                .status(AutomationExecution.Status.FAILED.name())
                .error("خطای آزمایشی")
                .startedAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build();
        when(tenantContext.requireTenantId()).thenReturn(42L);
        when(executions.findByTenantIdOrderByStartedAtDesc(eq(42L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(execution)));

        byte[] csv = service.executionReportCsv();

        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                java.util.Arrays.copyOf(csv, 3));
        assertTrue(new String(csv, StandardCharsets.UTF_8).contains("خطای آزمایشی"));
        verify(executions).findByTenantIdOrderByStartedAtDesc(eq(42L), any(Pageable.class));
    }

    @Test
    void deletePreservesExecutionHistoryAndDirectsUserToArchive() {
        AutomationRule rule = failure(8L, 42L).getRule();
        when(tenantContext.requireTenantId()).thenReturn(42L);
        when(rules.findWithActionsByIdAndTenantId(6L, 42L)).thenReturn(Optional.of(rule));
        when(executions.existsByRuleIdAndTenantId(6L, 42L)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service.delete(6L));

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, error.getErrorCode());
        assertTrue(error.getMessage().contains("archive"));
        verify(rules, never()).delete(any(AutomationRule.class));
        verifyNoInteractions(audit);
    }

    private AutomationFailure failure(Long id, Long tenantId) {
        AutomationRule rule = new AutomationRule();
        ReflectionTestUtils.setField(rule, "id", 6L);
        rule.setTenantId(tenantId);
        rule.setCode("customer-follow-up");
        rule.setTriggerType("crm.customer.CREATED");
        rule.setExecutionPolicy(Map.of("retryDelaySeconds", 30));
        AutomationFailure failure = AutomationFailure.builder()
                .tenantId(tenantId)
                .rule(rule)
                .executionId(12L)
                .payload(Map.of("customerId", 99))
                .nextRetryAt(Instant.now())
                .build();
        ReflectionTestUtils.setField(failure, "id", id);
        ReflectionTestUtils.setField(failure, "createdAt", Instant.now());
        return failure;
    }
}
