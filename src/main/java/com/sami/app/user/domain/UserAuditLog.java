package com.sami.app.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * One append-only audit entry for a user-management action.
 *
 * <p>Deliberately not a {@code BaseEntity} and deliberately without foreign keys:
 * audit rows are immutable facts that must survive even a permanent user delete,
 * so the affected user's and the acting user's emails are snapshotted alongside
 * their ids. Old/new value snapshots are JSONB maps of only the changed fields.
 */
@Entity
@Table(name = "user_audit_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    /** e.g. CREATED, UPDATED, STATUS_CHANGED, ARCHIVED, RESTORED, DELETED, PURGED. */
    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values")
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values")
    private Map<String, Object> newValues;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
