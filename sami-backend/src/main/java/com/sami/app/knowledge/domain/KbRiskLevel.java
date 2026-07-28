package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable SOP risk level; high risk can force approval on its own. */
@Entity @Table(name = "kb_risk_levels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbRiskLevel extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private java.math.BigDecimal weight;
    @Column(name = "requires_approval", nullable = false) private boolean requiresApproval;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
