package com.sami.app.user.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A configurable user lifecycle/business status.
 *
 * <p>As with roles and modules, behavior travels with data, never with names:
 * {@code allowsLogin} gates authentication, {@code hiddenByDefault} excludes the
 * status from default listings, {@code isDefault} marks the status assigned on
 * registration/reactivation/restore, and {@code isArchivedState} /
 * {@code isDeletedState} mark the (unique) targets of the archive and soft-delete
 * operations. Administrators may add, rename and reorder statuses freely; no
 * status code is ever referenced in application logic.
 */
@Entity
@Table(name = "user_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatus extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "allows_login", nullable = false)
    private boolean allowsLogin;

    @Column(name = "hidden_by_default", nullable = false)
    private boolean hiddenByDefault;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_archived_state", nullable = false)
    private boolean isArchivedState;

    @Column(name = "is_deleted_state", nullable = false)
    private boolean isDeletedState;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
