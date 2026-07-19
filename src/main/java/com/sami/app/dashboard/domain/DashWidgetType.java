package com.sami.app.dashboard.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable widget type (KPI card, chart, gauge, table, …). New types are rows. */
@Entity
@Table(name = "dash_widget_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashWidgetType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 64)
    private String icon;

    @Column(name = "chart_capable", nullable = false)
    private boolean chartCapable;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
