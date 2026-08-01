package com.sami.app.purchasing.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A configurable purchase status. Structural roles (draft, pending, approved,
 * partially received, completed, cancelled, rejected) are unique flagged rows;
 * behavior lives in {@code allowsEditing} / {@code allowsReceiving} /
 * {@code isTerminal}. No status code is referenced in application logic.
 */
@Entity
@Table(name = "pur_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurStatus extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "allows_editing", nullable = false)
    private boolean allowsEditing;

    @Column(name = "allows_receiving", nullable = false)
    private boolean allowsReceiving;

    @Column(name = "is_terminal", nullable = false)
    private boolean isTerminal;

    @Column(name = "is_draft_state", nullable = false)
    private boolean isDraftState;

    @Column(name = "is_pending_state", nullable = false)
    private boolean isPendingState;

    @Column(name = "is_approved_state", nullable = false)
    private boolean isApprovedState;

    @Column(name = "is_partial_state", nullable = false)
    private boolean isPartialState;

    @Column(name = "is_completed_state", nullable = false)
    private boolean isCompletedState;

    @Column(name = "is_cancelled_state", nullable = false)
    private boolean isCancelledState;

    @Column(name = "is_rejected_state", nullable = false)
    private boolean isRejectedState;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
