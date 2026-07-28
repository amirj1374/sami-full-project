package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configurable relationship kind. {@code inverseCode} lets a relation be
 * navigated both ways without storing it twice.
 */
@Entity @Table(name = "kb_relation_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbRelationType extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "inverse_code", length = 64) private String inverseCode;
    @Column(name = "is_symmetric", nullable = false) private boolean isSymmetric;
    /** ARTICLE | ENTITY | ANY */
    @Column(name = "target_kind", nullable = false, length = 32) private String targetKind;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
