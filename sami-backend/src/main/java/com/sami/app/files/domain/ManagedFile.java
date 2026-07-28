package com.sami.app.files.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The central file object. Business modules hold only {@link #fileUuid} — never
 * a path, bucket or provider — so storage can be reconfigured or migrated
 * without touching a single business table.
 *
 * <p>Named {@code ManagedFile} rather than {@code File} to avoid shadowing
 * {@link java.io.File} in every service that touches both.
 *
 * <p>The physical location lives on {@link FileVersion}, not here: moving a file
 * between providers creates a new version and never rewrites history.
 */
@Entity
@Table(name = "files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ManagedFile extends BaseEntity {

    @Column(name = "file_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID fileUuid = UUID.randomUUID();

    @Column(name = "file_code", nullable = false, length = 32)
    private String fileCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private FileCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private FileStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private FileFolder folder;

    @Column(name = "module_code", length = 64)
    private String moduleCode;

    @Column(name = "entity_code", length = 64)
    private String entityCode;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "owner_email", length = 255)
    private String ownerEmail;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Column(name = "size_bytes", nullable = false)
    @Builder.Default
    private long sizeBytes = 0L;

    @Column(length = 32)
    private String extension;

    @Column(name = "mime_type", length = 160)
    private String mimeType;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    /** Extracted metadata (resolution, duration, page count, …). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retention_policy_id")
    private RetentionPolicy retentionPolicy;

    @Column(name = "retention_expires_at")
    private Instant retentionExpiresAt;

    /** Overrides any retention policy — expiry can never delete a held file. */
    @Column(name = "legal_hold_until")
    private Instant legalHoldUntil;

    @Column(name = "uploaded_ip", length = 64)
    private String uploadedIp;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "restored_at")
    private Instant restoredAt;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public boolean isUnderLegalHold() {
        return legalHoldUntil != null && legalHoldUntil.isAfter(Instant.now());
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
