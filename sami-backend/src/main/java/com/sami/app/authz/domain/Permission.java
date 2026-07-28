package com.sami.app.authz.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single grantable action within a module.
 *
 * <p>{@code code} is the denormalized {@code <module.code>:<action>} identifier
 * (e.g. {@code users:view}) used everywhere as the granted authority — keeping it
 * on the row means authorities load without joining modules, with uniqueness
 * enforced at the database level.
 */
@Entity
@Table(name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_permissions_module_action",
                columnNames = {"module_id", "action"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private AppModule module;

    /** Lowercase action slug, e.g. {@code view} or {@code export}. */
    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, unique = true, length = 150)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;
}
