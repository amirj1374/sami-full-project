package com.sami.app.automation.domain;

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
 * Configurable automation lifecycle status. Only statuses flagged
 * {@code isActiveState} let a rule fire; new statuses are plain rows, no code
 * change required.
 */
@Entity
@Table(name = "automation_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationStatus extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    /** Rules in this status are eligible to execute. */
    @Column(name = "is_active_state", nullable = false)
    private boolean isActiveState;

    @Column(name = "is_archived_state", nullable = false)
    private boolean isArchivedState;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
