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
 * A thread of messages with a business context. The entity link is a soft
 * reference (module/type/id) resolved through the caller's world — never an
 * FK into business tables, the same rule as V24's schedules.
 */
@Entity @Table(name = "comm_conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommConversation extends BaseEntity {
    @Column(name = "conversation_number", nullable = false, length = 32) private String conversationNumber;
    @Column(length = 255) private String subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private CommChannel channel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private CommConversationStatus status;

    @Column(name = "owner_user_id") private Long ownerUserId;
    @Column(name = "customer_id") private Long customerId;
    @Column(name = "supplier_id") private Long supplierId;
    @Column(name = "module_code", length = 64) private String moduleCode;
    @Column(name = "related_entity_type", length = 64) private String relatedEntityType;
    @Column(name = "related_entity_id") private Long relatedEntityId;
    @Column(name = "started_at", nullable = false) @Builder.Default private Instant startedAt = Instant.now();
    @Column(name = "last_activity_at", nullable = false) @Builder.Default private Instant lastActivityAt = Instant.now();
    @Column(name = "closed_at") private Instant closedAt;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
