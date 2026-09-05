package com.sami.app.treasury.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** Data-driven lifecycle roles prevent application logic from relying on status codes. */
@Entity @Table(name = "treasury_transaction_statuses") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreasuryTransactionStatus extends BaseEntity {
    @Column(name = "tenant_id") private Long tenantId;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 255) private String description;
    @Column(name = "allows_editing", nullable = false) private boolean allowsEditing;
    @Column(name = "is_draft_state", nullable = false) private boolean isDraftState;
    @Column(name = "is_pending_state", nullable = false) private boolean isPendingState;
    @Column(name = "is_approved_state", nullable = false) private boolean isApprovedState;
    @Column(name = "is_completed_state", nullable = false) private boolean isCompletedState;
    @Column(name = "is_rejected_state", nullable = false) private boolean isRejectedState;
    @Column(name = "is_cancelled_state", nullable = false) private boolean isCancelledState;
    @Column(name = "is_terminal", nullable = false) private boolean isTerminal;
    @Builder.Default @Column(nullable = false) private boolean active = true;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
}
