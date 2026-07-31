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

import java.time.Instant;
import java.util.Map;

/** A detected data-quality problem, tracked until resolved or ignored. */
@Entity
@Table(name = "quality_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityIssue extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public enum Status { OPEN, RESOLVED, IGNORED }

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Column(name = "entity_code", nullable = false, length = 64)
    private String entityCode;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "field_name", length = 128)
    private String fieldName;

    @Column(name = "severity_code", nullable = false, length = 64)
    private String severityCode;

    @Column(name = "dimension_code", length = 64)
    private String dimensionCode;

    @Column(nullable = false, length = 1000)
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> detail;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = Status.OPEN.name();

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_by_email", length = 255)
    private String resolvedByEmail;
}
