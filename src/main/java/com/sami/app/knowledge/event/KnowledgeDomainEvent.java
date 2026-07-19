package com.sami.app.knowledge.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The module's published business event. Consumers subscribe with a plain Spring
 * {@code @EventListener}; automation reaches it through {@code DomainEventBridge}.
 */
public record KnowledgeDomainEvent(
        String eventId,
        String eventType,
        Long articleId,
        String articleCode,
        String moduleCode,
        String processCode,
        Map<String, Object> payload,
        Instant occurredAt
) {

    public static final String KNOWLEDGE_CREATED = "KnowledgeCreated";
    public static final String KNOWLEDGE_UPDATED = "KnowledgeUpdated";
    public static final String KNOWLEDGE_PUBLISHED = "KnowledgePublished";
    public static final String KNOWLEDGE_VIEWED = "KnowledgeViewed";
    public static final String KNOWLEDGE_ARCHIVED = "KnowledgeArchived";
    public static final String KNOWLEDGE_DEPRECATED = "KnowledgeDeprecated";
    public static final String SOP_APPROVED = "SOPApproved";
    public static final String APPROVAL_REQUESTED = "ApprovalRequested";
    public static final String APPROVAL_REJECTED = "ApprovalRejected";
    public static final String REVIEW_OVERDUE = "ReviewOverdue";

    public static KnowledgeDomainEvent of(String eventType, Long articleId, String articleCode,
                                          String moduleCode, String processCode,
                                          Map<String, Object> payload) {
        return new KnowledgeDomainEvent(UUID.randomUUID().toString(), eventType, articleId,
                articleCode, moduleCode, processCode,
                payload == null ? Map.of() : payload, Instant.now());
    }
}
