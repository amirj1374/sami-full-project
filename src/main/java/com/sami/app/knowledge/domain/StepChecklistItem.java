package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A tick-box within a procedure step. */
@Entity
@Table(name = "kb_step_checklist_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StepChecklistItem extends BaseEntity {

    @Column(name = "step_id", nullable = false) private Long stepId;
    @Column(nullable = false, length = 1000) private String text;
    @Column(name = "is_mandatory", nullable = false) @Builder.Default private boolean isMandatory = true;
    @Column(name = "display_order", nullable = false) private int displayOrder;
}
