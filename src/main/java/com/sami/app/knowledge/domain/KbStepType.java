package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable procedure step kind: sequential, parallel, decision, conditional. */
@Entity @Table(name = "kb_step_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbStepType extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_decision", nullable = false) private boolean isDecision;
    @Column(name = "is_parallel", nullable = false) private boolean isParallel;
    @Column(name = "is_conditional", nullable = false) private boolean isConditional;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
