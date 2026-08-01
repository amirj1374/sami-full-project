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

import java.time.Instant;

/**
 * One run of one job. Recorded even when the handler does nothing, so a sweep
 * that legitimately found no work is distinguishable from one that never ran.
 */
@Entity
@Table(name = "job_executions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobExecution extends BaseEntity {

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "execution_number", nullable = false, length = 32)
    private String executionNumber;

    /** SCHEDULED | MANUAL | STARTUP */
    @Column(name = "trigger_kind", nullable = false, length = 32)
    @Builder.Default
    private String triggerKind = "SCHEDULED";

    /** RUNNING | SUCCEEDED | FAILED | TIMED_OUT | SKIPPED */
    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "RUNNING";

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(length = 2000)
    private String outcome;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "items_processed")
    private Integer itemsProcessed;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
}
