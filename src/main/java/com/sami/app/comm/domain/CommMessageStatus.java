package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message lifecycle state. The sweep reads {@code allowsRetry}; reporting
 * reads {@code countsAsSuccess}; nothing anywhere reads the code.
 */
@Entity @Table(name = "comm_message_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommMessageStatus extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 16) private String color;
    @Column(name = "is_queued_state", nullable = false) private boolean isQueuedState;
    @Column(name = "is_sending_state", nullable = false) private boolean isSendingState;
    @Column(name = "is_sent_state", nullable = false) private boolean isSentState;
    @Column(name = "is_delivered_state", nullable = false) private boolean isDeliveredState;
    @Column(name = "is_read_state", nullable = false) private boolean isReadState;
    @Column(name = "is_failed_state", nullable = false) private boolean isFailedState;
    @Column(name = "is_expired_state", nullable = false) private boolean isExpiredState;
    @Column(name = "is_cancelled_state", nullable = false) private boolean isCancelledState;
    @Column(name = "allows_retry", nullable = false) private boolean allowsRetry;
    @Column(name = "is_terminal", nullable = false) private boolean isTerminal;
    @Column(name = "counts_as_success", nullable = false) private boolean countsAsSuccess;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
