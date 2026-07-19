package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Grouping for appointments; presentation and reporting only. */
@Entity @Table(name = "schedule_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleCategory extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    @Column(length = 64) private String icon;
    @Column(length = 16) private String color;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
