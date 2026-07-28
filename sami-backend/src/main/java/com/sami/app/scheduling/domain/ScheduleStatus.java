package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Appointment lifecycle state.
 *
 * <p>{@code blocksResource} is the load-bearing flag: it decides whether an
 * appointment in this status occupies its resources, and is mirrored onto
 * {@code reservations.holds_resource}, which the database exclusion constraint
 * predicates on. A terminal status must never block — the migration asserts it.
 */
@Entity @Table(name = "schedule_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleStatus extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 16) private String color;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_draft_state", nullable = false) private boolean isDraftState;
    @Column(name = "is_confirmed_state", nullable = false) private boolean isConfirmedState;
    @Column(name = "is_checked_in_state", nullable = false) private boolean isCheckedInState;
    @Column(name = "is_in_progress_state", nullable = false) private boolean isInProgressState;
    @Column(name = "is_completed_state", nullable = false) private boolean isCompletedState;
    @Column(name = "is_cancelled_state", nullable = false) private boolean isCancelledState;
    @Column(name = "is_no_show_state", nullable = false) private boolean isNoShowState;
    @Column(name = "is_archived_state", nullable = false) private boolean isArchivedState;
    @Column(name = "blocks_resource", nullable = false) private boolean blocksResource;
    @Column(name = "allows_edit", nullable = false) private boolean allowsEdit;
    @Column(name = "allows_cancel", nullable = false) private boolean allowsCancel;
    @Column(name = "allows_check_in", nullable = false) private boolean allowsCheckIn;
    @Column(name = "is_terminal", nullable = false) private boolean isTerminal;
    @Column(name = "counts_as_attended", nullable = false) private boolean countsAsAttended;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
