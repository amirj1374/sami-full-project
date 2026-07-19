package com.sami.app.automation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/** Append-only per-action step log within an {@link AutomationExecution}. */
@Entity
@Table(name = "automation_execution_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "action_type", length = 128)
    private String actionType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 1000)
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();
}
