package com.sami.app.dataquality.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** One validation pass over one entity payload — append-only history. */
@Entity
@Table(name = "validation_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationRun extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public enum Status { PASSED, FAILED }

    @Column(name = "run_number", nullable = false, unique = true, length = 40)
    private String runNumber;

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Column(name = "entity_code", nullable = false, length = 64)
    private String entityCode;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "rule_count", nullable = false)
    private int ruleCount;

    @Column(name = "passed_count", nullable = false)
    private int passedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "band_code", length = 64)
    private String bandCode;

    /** True when at least one failure came from a severity that blocks saving. */
    @Column(nullable = false)
    private boolean blocking;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> payload;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "executed_by")
    private Long executedBy;

    @Column(name = "executed_by_email", length = 255)
    private String executedByEmail;
}
