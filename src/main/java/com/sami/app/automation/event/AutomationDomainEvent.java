package com.sami.app.automation.event;

import java.time.Instant;
import java.util.Map;

/**
 * Published by the automation module on lifecycle and execution transitions, for
 * decoupled downstream consumers (audit, notifications, future AI/process-mining).
 * Consumers subscribe with a plain Spring {@code @EventListener}.
 */
public record AutomationDomainEvent(
        String eventId,
        String eventType,
        Long ruleId,
        String ruleCode,
        Long executionId,
        Map<String, Object> payload,
        Instant occurredAt
) {
    public static final String RULE_CREATED = "RuleCreated";
    public static final String RULE_UPDATED = "RuleUpdated";
    public static final String RULE_ACTIVATED = "RuleActivated";
    public static final String RULE_DISABLED = "RuleDisabled";
    public static final String EXECUTION_STARTED = "ExecutionStarted";
    public static final String EXECUTION_COMPLETED = "ExecutionCompleted";
    public static final String EXECUTION_FAILED = "ExecutionFailed";
}
