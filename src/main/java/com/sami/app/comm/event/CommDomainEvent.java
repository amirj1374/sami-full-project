package com.sami.app.comm.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The module's published business event. Consumers subscribe with a plain
 * Spring {@code @EventListener}; automation reaches it via DomainEventBridge.
 */
public record CommDomainEvent(
        String eventId,
        String eventType,
        Long messageId,
        String messageNumber,
        Long conversationId,
        String channelCode,
        String moduleCode,
        Map<String, Object> payload,
        Instant occurredAt
) {
    public static final String MESSAGE_QUEUED = "MessageQueued";
    public static final String MESSAGE_SENT = "MessageSent";
    public static final String MESSAGE_DELIVERED = "MessageDelivered";
    public static final String MESSAGE_READ = "MessageRead";
    public static final String MESSAGE_FAILED = "MessageFailed";
    public static final String MESSAGE_EXPIRED = "MessageExpired";
    public static final String CONVERSATION_STARTED = "ConversationStarted";
    public static final String CONVERSATION_CLOSED = "ConversationClosed";

    public static CommDomainEvent of(String eventType, Long messageId, String messageNumber,
                                     Long conversationId, String channelCode, String moduleCode,
                                     Map<String, Object> payload) {
        return new CommDomainEvent(UUID.randomUUID().toString(), eventType, messageId, messageNumber,
                conversationId, channelCode, moduleCode,
                payload == null ? Map.of() : payload, Instant.now());
    }
}
