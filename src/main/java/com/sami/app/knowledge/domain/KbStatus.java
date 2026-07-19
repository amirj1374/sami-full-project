package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configurable article lifecycle state. {@code isVisible} is what makes an
 * article readable, so deprecating one hides it without any service testing for
 * a status name.
 */
@Entity @Table(name = "kb_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbStatus extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_draft_state", nullable = false) private boolean isDraftState;
    @Column(name = "is_review_state", nullable = false) private boolean isReviewState;
    @Column(name = "is_approved_state", nullable = false) private boolean isApprovedState;
    @Column(name = "is_published_state", nullable = false) private boolean isPublishedState;
    @Column(name = "is_deprecated_state", nullable = false) private boolean isDeprecatedState;
    @Column(name = "is_archived_state", nullable = false) private boolean isArchivedState;
    @Column(name = "allows_edit", nullable = false) private boolean allowsEdit;
    @Column(name = "is_visible", nullable = false) private boolean isVisible;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
