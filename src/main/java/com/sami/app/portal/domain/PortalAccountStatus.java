package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable portal account lifecycle. {@code allowsLogin} is the single gate. */
@Entity @Table(name = "portal_account_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalAccountStatus extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "allows_login", nullable = false) private boolean allowsLogin;
    @Column(name = "requires_verification", nullable = false) private boolean requiresVerification;
    @Column(name = "is_locked_state", nullable = false) private boolean isLockedState;
    @Column(name = "is_suspended_state", nullable = false) private boolean isSuspendedState;
    @Column(name = "is_archived_state", nullable = false) private boolean isArchivedState;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
