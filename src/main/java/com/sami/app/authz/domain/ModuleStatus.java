package com.sami.app.authz.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A configurable module lifecycle stage.
 *
 * <p>Behaviour is read from the flags, never from {@code code}: the frontend
 * shows a placeholder because {@code showsPlaceholder} is set, not because the
 * status happens to be called {@code PLANNED}. Inserting a new stage is
 * therefore a data change.
 *
 * <p>{@code lifecycleRank} orders the stages so the overall status can be
 * derived without any service knowing the stage names — see
 * {@code ModuleLifecycle}.
 */
@Entity
@Table(name = "module_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModuleStatus extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    @Column(length = 16) private String color;
    @Column(length = 64) private String icon;

    /** Lower is earlier in the lifecycle. Gaps allow later insertion. */
    @Column(name = "lifecycle_rank", nullable = false) private int lifecycleRank;

    @Column(name = "applies_to_backend", nullable = false) private boolean appliesToBackend;
    @Column(name = "applies_to_frontend", nullable = false) private boolean appliesToFrontend;

    @Column(name = "is_navigable", nullable = false) private boolean isNavigable;
    @Column(name = "shows_placeholder", nullable = false) private boolean showsPlaceholder;
    @Column(name = "is_production_ready", nullable = false) private boolean isProductionReady;
    @Column(name = "is_terminal", nullable = false) private boolean isTerminal;

    @Column(name = "is_default_backend", nullable = false) private boolean isDefaultBackend;
    @Column(name = "is_default_frontend", nullable = false) private boolean isDefaultFrontend;

    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
