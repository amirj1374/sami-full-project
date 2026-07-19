package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/**
 * A customer's portal account.
 *
 * <p>Bound to a CRM {@code customers} row, one per customer — two accounts for
 * one customer would make "own data only" ambiguous. Lockout state lives here
 * because this is the first internet-facing login surface in the system.
 */
@Entity @Table(name = "portal_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalAccount extends BaseEntity {
    @Column(name = "customer_id", nullable = false) private Long customerId;
    @Column(nullable = false, length = 160) private String username;
    @Column(name = "mobile_number", length = 32) private String mobileNumber;
    @Column(length = 255) private String email;
    @Column(name = "password_hash", length = 255) private String passwordHash;
    @Column(name = "preferred_language", nullable = false, length = 16) @Builder.Default
    private String preferredLanguage = "fa";
    @Column(name = "preferred_timezone", nullable = false, length = 64) @Builder.Default
    private String preferredTimezone = "Asia/Tehran";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private PortalAccountStatus status;

    @Column(name = "registered_at", nullable = false) @Builder.Default
    private Instant registeredAt = Instant.now();
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "last_login_at") private Instant lastLoginAt;
    @Column(name = "last_login_ip", length = 64) private String lastLoginIp;
    @Column(name = "failed_attempts", nullable = false) @Builder.Default private int failedAttempts = 0;
    @Column(name = "locked_until") private Instant lockedUntil;
    @Column(name = "must_change_password", nullable = false) private boolean mustChangePassword;
    @Column(name = "marketing_opt_in", nullable = false) private boolean marketingOptIn;
    @Column(name = "invited_by") private Long invitedBy;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    /** Temporary lockout from failed attempts, independent of the status. */
    public boolean isTemporarilyLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public boolean canLogIn() {
        return status != null && status.isAllowsLogin() && !isTemporarilyLocked();
    }
}
