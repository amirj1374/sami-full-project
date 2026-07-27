package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Conversation lifecycle state. */
@Entity @Table(name = "comm_conversation_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommConversationStatus extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 16) private String color;
    @Column(name = "is_open_state", nullable = false) private boolean isOpenState;
    @Column(name = "allows_reply", nullable = false) private boolean allowsReply;
    @Column(name = "is_terminal", nullable = false) private boolean isTerminal;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
