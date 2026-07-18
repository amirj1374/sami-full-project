package com.sami.app.user.domain;

import com.sami.app.authz.domain.Role;
import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Application user / account.
 *
 * <p>Password is always stored BCrypt-hashed (never in plain text). Email is the
 * unique login identifier. Every user has exactly one {@link Role} (permissions)
 * and one {@link UserStatus} (lifecycle/business state — whether the account may
 * log in is a property of its status, not a hardcoded flag). Archive and
 * soft-delete additionally record who/when in the lifecycle columns; rows are
 * only physically removed by the explicit, permission-guarded purge operation.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private UserStatus status;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private Long archivedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    /** Convenience mirror of the old flag: login is a property of the status. */
    public boolean isLoginAllowed() {
        return status.isAllowsLogin();
    }
}
