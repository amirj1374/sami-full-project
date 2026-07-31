package com.sami.app.automation.engine;

import com.sami.app.automation.domain.AutomationRule;
import com.sami.app.automation.domain.AutomationExecution;
import com.sami.app.automation.repository.AutomationRuleRepository;
import com.sami.app.automation.spi.AutomationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The engine core: given a firing {@link AutomationContext}, selects the active
 * rules whose {@code triggerType} matches and runs each through
 * {@link RuleExecutor}. Adding triggers/actions/conditions never touches this
 * class — matching is by data (the trigger key), execution is by SPI beans.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationEngine {

    private final AutomationRuleRepository ruleRepository;
    private final RuleExecutor ruleExecutor;

    /** Fan a firing context out to every matching active rule. */
    public void dispatch(AutomationContext ctx) {
        if (ctx == null || ctx.tenantId() == null) {
            log.warn("Automation dispatch rejected because trusted tenant scope is missing");
            return;
        }
        for (AutomationRule rule : ruleRepository.findActiveRules(ctx.tenantId())) {
            if (!triggerMatches(rule.getTriggerType(), ctx.triggerType())) {
                continue;
            }
            try {
                ruleExecutor.execute(rule.getId(), ctx);
            } catch (RuntimeException ex) {
                // One rule's failure must never block the others.
                log.warn("Automation rule {} failed: {}", rule.getCode(), ex.getMessage());
            }
        }
    }

    /** Run a specific rule directly (manual execution / testing), bypassing trigger match. */
    public AutomationExecution.Status executeRule(Long ruleId, AutomationContext ctx) {
        if (ctx == null || ctx.tenantId() == null) {
            throw new IllegalArgumentException("Trusted tenant scope is required for automation execution");
        }
        return ruleExecutor.execute(ruleId, ctx);
    }

    /**
     * Trigger matching: exact key, global {@code *}, or a dotted wildcard suffix
     * ({@code crm.customer.*} matches {@code crm.customer.CREATED}).
     */
    boolean triggerMatches(String pattern, String actual) {
        if (pattern == null || pattern.isBlank() || actual == null) {
            return false;
        }
        if (pattern.equals(actual) || pattern.equals("*")) {
            return true;
        }
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 1); // keep trailing dot
            return actual.startsWith(prefix);
        }
        return false;
    }
}
