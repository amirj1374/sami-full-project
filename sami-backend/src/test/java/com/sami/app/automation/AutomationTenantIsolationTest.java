package com.sami.app.automation;

import com.sami.app.automation.engine.AutomationEngine;
import com.sami.app.automation.engine.RuleExecutor;
import com.sami.app.automation.repository.AutomationRuleRepository;
import com.sami.app.automation.spi.AutomationContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class AutomationTenantIsolationTest {

    private final AutomationRuleRepository rules = mock(AutomationRuleRepository.class);
    private final RuleExecutor executor = mock(RuleExecutor.class);
    private final AutomationEngine engine = new AutomationEngine(rules, executor);

    @Test
    void dispatchFailsClosedWithoutTrustedTenant() {
        engine.dispatch(context(null));

        verifyNoInteractions(rules, executor);
    }

    @Test
    void dispatchSelectsRulesOnlyFromTrustedTenant() {
        when(rules.findActiveRules(42L)).thenReturn(List.of());

        engine.dispatch(context(42L));

        verify(rules).findActiveRules(42L);
        verifyNoInteractions(executor);
    }

    @Test
    void recursionCopyPreservesTenantScope() {
        AutomationContext original = context(73L);

        AutomationContext deeper = original.deeper();

        org.junit.jupiter.api.Assertions.assertEquals(73L, deeper.tenantId());
        org.junit.jupiter.api.Assertions.assertEquals(1, deeper.depth());
    }

    private AutomationContext context(Long tenantId) {
        return new AutomationContext("crm.customer.CREATED", "crm", "customer", 1L,
                Map.of(), tenantId, null, null, 7L, Instant.now(), 0);
    }
}
