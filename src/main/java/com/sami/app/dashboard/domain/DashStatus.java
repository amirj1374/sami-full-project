package com.sami.app.dashboard.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable dashboard lifecycle status (Draft/Active/Inactive/Archived). */
@Entity
@Table(name = "dash_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashStatus extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_active_state", nullable = false)
    private boolean isActiveState;

    @Column(name = "is_archived_state", nullable = false)
    private boolean isArchivedState;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
