package com.sami.app.dashboard.domain;

import com.sami.app.common.domain.BaseEntity;
import com.sami.app.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A configurable KPI definition. {@code calculationMethod} selects a
 * {@link com.sami.app.dashboard.spi.KpiCalculationProvider} (PROVIDER / FORMULA
 * / MANUAL / future strategies); {@code formula} is interpreted by that
 * strategy. Threshold levels are child rows, values are stored historically.
 */
@Entity
@Table(name = "kpi_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiDefinition extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "calculation_method", nullable = false, length = 32)
    @Builder.Default
    private String calculationMethod = "PROVIDER";

    @Column(length = 1000)
    private String formula;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String aggregation = "LAST";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id")
    private DashDataSource dataSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refresh_policy_id")
    private DashRefreshPolicy refreshPolicy;

    @Column(name = "target_value", precision = 18, scale = 4)
    private BigDecimal targetValue;

    @Column(length = 32)
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private DashKpiStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_email", length = 255)
    private String createdByEmail;

    @OneToMany(mappedBy = "kpi", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<KpiThreshold> thresholds = new ArrayList<>();
}
