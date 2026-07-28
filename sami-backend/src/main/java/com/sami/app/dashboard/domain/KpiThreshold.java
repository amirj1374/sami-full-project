package com.sami.app.dashboard.domain;

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

import java.math.BigDecimal;

/**
 * One configurable threshold band of a KPI (Excellent/Good/Warning/Critical/…).
 * A value falls in this band when {@code minValue <= v < maxValue} (null bounds
 * are open). Levels are evaluated in {@code sortOrder}.
 */
@Entity
@Table(name = "kpi_thresholds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiThreshold extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kpi_id", nullable = false)
    private KpiDefinition kpi;

    @Column(name = "level_name", nullable = false, length = 64)
    private String levelName;

    @Column(length = 32)
    private String color;

    @Column(name = "min_value", precision = 18, scale = 4)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 18, scale = 4)
    private BigDecimal maxValue;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
