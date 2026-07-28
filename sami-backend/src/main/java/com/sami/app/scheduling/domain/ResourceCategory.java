package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What a resource is, and which scheduling rules follow.
 *
 * <p>Services read {@code isHuman} / {@code requiresSkillMatch} rather than the
 * code, so adding "Diagnostic Rig" needs no code change.
 */
@Entity @Table(name = "resource_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResourceCategory extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    @Column(length = 64) private String icon;
    @Column(name = "is_human", nullable = false) private boolean isHuman;
    @Column(name = "is_physical_space", nullable = false) private boolean isPhysicalSpace;
    @Column(name = "is_equipment", nullable = false) private boolean isEquipment;
    @Column(name = "supports_capacity", nullable = false) private boolean supportsCapacity;
    @Column(name = "requires_skill_match", nullable = false) private boolean requiresSkillMatch;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
