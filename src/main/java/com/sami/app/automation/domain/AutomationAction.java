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

import java.util.HashMap;
import java.util.Map;

/**
 * One ordered step of a rule's workflow. {@code actionType} resolves to an
 * {@code ActionProvider} bean; {@code config} and {@code stepCondition} are open
 * JSON. {@code runMode}/{@code continueOnError}/{@code delaySeconds}/
 * {@code retryCount} drive sequential/parallel/conditional/delayed/retry
 * execution — all configuration, no hardcoded flow.
 */
@Entity
@Table(name = "automation_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationAction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private AutomationRule rule;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "action_type", nullable = false, length = 128)
    private String actionType;

    @Column(length = 160)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    /** Optional per-step condition (same shape as the rule condition tree). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "step_condition")
    private Map<String, Object> stepCondition;

    @Column(name = "run_mode", nullable = false, length = 32)
    @Builder.Default
    private String runMode = "SEQUENTIAL";

    @Column(name = "continue_on_error", nullable = false)
    private boolean continueOnError;

    @Column(name = "delay_seconds", nullable = false)
    @Builder.Default
    private int delaySeconds = 0;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;
}
