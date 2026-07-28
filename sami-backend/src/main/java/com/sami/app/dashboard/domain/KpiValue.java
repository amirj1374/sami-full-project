package com.sami.app.dashboard.domain;

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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One historical KPI calculation (append-only). Enables trend analysis and
 * recalculation; {@code periodKey} scopes a value to a period (e.g. a date),
 * {@code thresholdLevel} snapshots the band it fell into at compute time.
 */
@Entity
@Table(name = "kpi_values")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kpi_id", nullable = false)
    private Long kpiId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal value;

    @Column(name = "period_key", nullable = false, length = 32)
    @Builder.Default
    private String periodKey = "ALL";

    @Column(name = "threshold_level", length = 64)
    private String thresholdLevel;

    @Column(name = "computed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant computedAt = Instant.now();
}
