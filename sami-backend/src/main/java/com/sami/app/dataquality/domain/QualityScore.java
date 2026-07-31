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

/** A computed score snapshot, kept historically for trend analysis. */
@Entity
@Table(name = "quality_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityScore extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Column(name = "entity_code", nullable = false, length = 64)
    private String entityCode;

    @Column(name = "dimension_code", length = 64)
    private String dimensionCode;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "band_code", length = 64)
    private String bandCode;

    @Column(name = "sample_size", nullable = false)
    private int sampleSize;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> detail;

    @Column(name = "computed_at", nullable = false)
    @Builder.Default
    private Instant computedAt = Instant.now();
}
