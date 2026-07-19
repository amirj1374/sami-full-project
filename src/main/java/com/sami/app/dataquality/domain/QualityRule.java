package com.sami.app.dataquality.domain;

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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * A data-quality rule — pure configuration. {@code validationType} resolves to a
 * {@code ValidationRule} plugin bean, {@code config} carries its parameters,
 * {@code conditionConfig} makes the rule conditional, and {@code weight} plus the
 * severity/dimension weights feed the quality score. New rule kinds are new
 * plugins; new rules are new rows.
 */
@Entity
@Table(name = "quality_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityRule extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Column(name = "entity_code", nullable = false, length = 64)
    private String entityCode;

    @Column(length = 64)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 100;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private QualityStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "severity_id", nullable = false)
    private QualitySeverity severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dimension_id")
    private QualityDimension dimension;

    @Column(name = "validation_type", nullable = false, length = 128)
    private String validationType;

    @Column(name = "target_field", length = 128)
    private String targetField;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_config", nullable = false)
    @Builder.Default
    private Map<String, Object> conditionConfig = new HashMap<>();

    @Column(nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_email", length = 255)
    private String createdByEmail;
}
