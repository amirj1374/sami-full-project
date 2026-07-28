package com.sami.app.automation.domain;

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
import java.util.Map;

/** One execution of a rule — append-only history for auditing and diagnostics. */
@Entity
@Table(name = "automation_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationExecution extends BaseEntity {

    public enum Status { RUNNING, SUCCEEDED, FAILED, SKIPPED }

    @Column(name = "execution_number", nullable = false, unique = true, length = 40)
    private String executionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private AutomationRule rule;

    @Column(name = "trigger_type", nullable = false, length = 128)
    private String triggerType;

    @Column(name = "trigger_ref", length = 160)
    private String triggerRef;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 32)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> context;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> result;

    @Column(length = 2000)
    private String error;

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
