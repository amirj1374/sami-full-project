package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * The permanent record of one communication. Written FIRST, delivered second
 * (transactional-outbox shape): a dispatch failure can never lose a message,
 * only leave it queued for the sweep.
 */
@Entity @Table(name = "comm_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommMessage extends BaseEntity {
    @Column(name = "message_number", nullable = false, length = 32) private String messageNumber;
    @Column(name = "conversation_id") private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private CommChannel channel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private CommMessageStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private CommTemplate template;

    @Column(nullable = false, length = 16) @Builder.Default private String direction = "OUTBOUND";
    @Column(name = "sender_user_id") private Long senderUserId;
    @Column(name = "recipient_address", nullable = false, length = 255) private String recipientAddress;
    @Column(name = "customer_id") private Long customerId;
    @Column(name = "supplier_id") private Long supplierId;
    @Column(name = "module_code", length = 64) private String moduleCode;
    @Column(name = "related_entity_type", length = 64) private String relatedEntityType;
    @Column(name = "related_entity_id") private Long relatedEntityId;
    @Column(length = 500) private String subject;
    @Column(nullable = false, columnDefinition = "text") private String body;
    @Column(nullable = false, length = 16) @Builder.Default private String language = "fa";
    @Column(nullable = false) private int priority;
    @Column(name = "idempotency_key", length = 128) private String idempotencyKey;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at") private Instant nextAttemptAt;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(name = "provider_message_ref", length = 255) private String providerMessageRef;
    @Column(name = "queued_at", nullable = false) @Builder.Default private Instant queuedAt = Instant.now();
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "delivered_at") private Instant deliveredAt;
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "failed_at") private Instant failedAt;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
