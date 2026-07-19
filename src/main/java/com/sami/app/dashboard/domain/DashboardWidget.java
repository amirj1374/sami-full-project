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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * One widget on a dashboard. The widget type, chart type, data source and
 * refresh policy are all configurable references; {@code config} (JSONB) carries
 * type-specific settings (which metric, thresholds, columns…). An optional
 * {@code requiredPermission} hides the widget from users lacking it.
 */
@Entity
@Table(name = "dashboard_widgets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWidget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @Column(nullable = false, length = 64)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_type_id", nullable = false)
    private DashWidgetType widgetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chart_type_id")
    private DashChartType chartType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kpi_id")
    private KpiDefinition kpi;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id")
    private DashDataSource dataSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refresh_policy_id")
    private DashRefreshPolicy refreshPolicy;

    @Column(name = "position_x", nullable = false)
    private int positionX;

    @Column(name = "position_y", nullable = false)
    private int positionY;

    @Column(nullable = false)
    @Builder.Default
    private int width = 3;

    @Column(nullable = false)
    @Builder.Default
    private int height = 2;

    @Column(name = "required_permission", length = 100)
    private String requiredPermission;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();
}
