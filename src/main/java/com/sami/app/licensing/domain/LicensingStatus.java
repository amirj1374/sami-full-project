package com.sami.app.licensing.domain;

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
 * Configurable status, scoped per aggregate ({@code LICENSE}, {@code TENANT},
 * {@code PLAN}). Behaviour travels with flags, never with names: only a status
 * whose {@code grantsAccess} is true lets licensed functionality through.
 */
@Entity
@Table(name = "licensing_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicensingStatus extends BaseEntity {

    public static final String SCOPE_LICENSE = "LICENSE";
    public static final String SCOPE_TENANT = "TENANT";
    public static final String SCOPE_PLAN = "PLAN";

    @Column(nullable = false, length = 16)
    private String scope;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    /** Licensed functionality is permitted while the holder is in this status. */
    @Column(name = "grants_access", nullable = false)
    private boolean grantsAccess;

    @Column(name = "is_expired_state", nullable = false)
    private boolean isExpiredState;

    @Column(name = "is_grace_state", nullable = false)
    private boolean isGraceState;

    @Column(name = "is_blocked_state", nullable = false)
    private boolean isBlockedState;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
