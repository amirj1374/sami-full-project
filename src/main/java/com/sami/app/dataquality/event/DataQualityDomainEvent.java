package com.sami.app.dataquality.event;

import java.time.Instant;
import java.util.Map;

/**
 * Published on validation and issue transitions so downstream consumers
 * (automation rules, notifications, dashboards, future AI analysis) react
 * without this module knowing about them.
 */
public record DataQualityDomainEvent(
        String eventId,
        String eventType,
        String moduleCode,
        String entityCode,
        Long entityId,
        Map<String, Object> payload,
        Instant occurredAt
) {
    public static final String VALIDATION_COMPLETED = "ValidationCompleted";
    public static final String ISSUE_DETECTED = "IssueDetected";
    public static final String ISSUE_RESOLVED = "IssueResolved";
    public static final String DUPLICATE_DETECTED = "DuplicateDetected";
    public static final String QUALITY_SCORE_UPDATED = "QualityScoreUpdated";
    public static final String AUTOMATIC_CORRECTION_EXECUTED = "AutomaticCorrectionExecuted";
}
