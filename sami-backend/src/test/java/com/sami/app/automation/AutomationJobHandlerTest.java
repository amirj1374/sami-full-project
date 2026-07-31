package com.sami.app.automation;

import com.sami.app.automation.engine.AutomationEngine;
import com.sami.app.automation.domain.AutomationExecution;
import com.sami.app.automation.provider.AutomationJobHandler;
import com.sami.app.automation.spi.AutomationContext;
import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.tenancy.TenantContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AutomationJobHandlerTest {

    private final AutomationEngine engine = mock(AutomationEngine.class);
    private final AutomationJobHandler handler = new AutomationJobHandler(engine, new TenantContext());

    @Test
    void rejectsMissingRuleConfiguration() {
        var result = handler.execute(context(Map.of(), 4L));
        assertFalse(result.success());
        verifyNoInteractions(engine);
    }

    @Test
    void rejectsMissingTenant() {
        var result = handler.execute(context(Map.of("ruleId", 9), null));
        assertFalse(result.success());
        verifyNoInteractions(engine);
    }

    @Test
    void executesConfiguredRuleWithSchedulerTenant() {
        when(engine.executeRule(eq(9L), any())).thenReturn(AutomationExecution.Status.SUCCEEDED);
        var result = handler.execute(context(Map.of("ruleId", 9, "branchId", 3), 44L));

        assertTrue(result.success());
        verify(engine).executeRule(eq(9L), argThat((AutomationContext ctx) ->
                ctx.tenantId().equals(44L) && ctx.branchId().equals(3L)
                        && ctx.triggerType().equals("automation.schedule.DUE")));
    }

    private JobContext context(Map<String, Object> config, Long tenantId) {
        return new JobContext("daily-automation", config, tenantId,
                Instant.parse("2026-07-31T00:00:00Z"), false, null);
    }
}
