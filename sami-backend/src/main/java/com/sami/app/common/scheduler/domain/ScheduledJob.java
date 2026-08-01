package com.sami.app.common.scheduler.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A scheduled unit of work. {@code handlerKey} resolves the {@link
 * com.sami.app.common.scheduler.spi.JobHandler} bean that performs it.
 *
 * <p>Jobs are rows rather than annotations so they can be listed, paused,
 * retimed and audited, and so each run can be wrapped in a tenant context.
 */
@Entity
@Table(name = "scheduled_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduledJob extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "handler_key", nullable = false, length = 64)
    private String handlerKey;

    /** CRON | FIXED_DELAY | FIXED_RATE | ONCE */
    @Column(name = "schedule_kind", nullable = false, length = 32)
    @Builder.Default
    private String scheduleKind = "CRON";

    @Column(name = "cron_expression", length = 160)
    private String cronExpression;

    @Column(name = "interval_seconds")
    private Integer intervalSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private JobStatus status;

    @Column(nullable = false, length = 64)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_status", length = 32)
    private String lastStatus;

    @Column(name = "last_duration_ms")
    private Long lastDurationMs;

    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private int consecutiveFailures = 0;

    /** After this many consecutive failures the job auto-pauses. */
    @Column(name = "max_failures", nullable = false)
    @Builder.Default
    private int maxFailures = 5;

    @Column(name = "timeout_seconds", nullable = false)
    @Builder.Default
    private int timeoutSeconds = 300;

    /**
     * When false, a job whose window was missed runs once at the next
     * opportunity rather than replaying every missed occurrence.
     */
    @Column(name = "catch_up", nullable = false)
    private boolean catchUp;

    @Column(name = "run_on_startup", nullable = false)
    private boolean runOnStartup;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
}
