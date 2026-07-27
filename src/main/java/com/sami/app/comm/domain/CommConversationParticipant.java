package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One party in a conversation: a staff user, customer, supplier or outsider. */
@Entity @Table(name = "comm_conversation_participants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommConversationParticipant extends BaseEntity {
    @Column(name = "conversation_id", nullable = false) private Long conversationId;
    /** USER | CUSTOMER | SUPPLIER | EXTERNAL */
    @Column(name = "participant_kind", nullable = false, length = 32) private String participantKind;
    @Column(name = "participant_id") private Long participantId;
    @Column(name = "display_name", length = 255) private String displayName;
    @Column(length = 255) private String address;
    @Column(name = "joined_at", nullable = false) @Builder.Default private Instant joinedAt = Instant.now();
}
