package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One step in the configurable approval chain. {@code stageOrder} defines the
 * sequence; {@code isFinal} marks the stage whose approval permits publication.
 */
@Entity @Table(name = "kb_approval_stages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbApprovalStage extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "stage_order", nullable = false) private int stageOrder;
    /** Permission code an approver must hold; checked against {@code @authz}. */
    @Column(name = "required_permission", length = 120) private String requiredPermission;
    @Column(name = "is_final", nullable = false) private boolean isFinal;
    @Column(name = "is_optional", nullable = false) private boolean isOptional;
    @Column(name = "requires_signature", nullable = false) private boolean requiresSignature;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "tenant_id") private Long tenantId;
}
