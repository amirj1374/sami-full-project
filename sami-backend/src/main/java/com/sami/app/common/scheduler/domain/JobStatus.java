package com.sami.app.common.scheduler.domain;

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
 * Configurable job lifecycle state. The runner selects work by
 * {@code allowsRun}, so pausing a job is a status change rather than a branch.
 */
@Entity
@Table(name = "job_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobStatus extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "allows_run", nullable = false)
    private boolean allowsRun;

    @Column(name = "is_paused_state", nullable = false)
    private boolean isPausedState;

    @Column(name = "is_failed_state", nullable = false)
    private boolean isFailedState;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "tenant_id")
    private Long tenantId;
}
