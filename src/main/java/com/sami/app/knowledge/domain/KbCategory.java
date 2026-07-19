package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable article category. {@code reviewMonths} drives the review-due report. */
@Entity @Table(name = "kb_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbCategory extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 500) private String description;
    @Column(length = 64) private String icon;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id")
    private KbCategory parent;

    @Column(name = "requires_approval", nullable = false) private boolean requiresApproval;
    /** Articles in this category are procedures and carry SOP structure. */
    @Column(name = "is_procedure", nullable = false) private boolean isProcedure;
    @Column(name = "review_months") private Integer reviewMonths;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
