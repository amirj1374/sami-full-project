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

import java.util.HashMap;
import java.util.Map;

/**
 * An immutable stored revision. Rollback promotes an existing version rather
 * than copying bytes, so history is never destroyed and a restore is atomic.
 */
@Entity
@Table(name = "file_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileVersion extends BaseEntity {

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "version_major", nullable = false)
    @Builder.Default
    private int versionMajor = 1;

    @Column(name = "version_minor", nullable = false)
    @Builder.Default
    private int versionMinor = 0;

    @Column(nullable = false)
    @Builder.Default
    private int revision = 0;

    /** Denormalised "1.0.0" for display and stable lookup. */
    @Column(nullable = false, length = 64)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private StorageProviderConfig provider;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "mime_type", length = 160)
    private String mimeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Column(length = 1000)
    private String comment;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_email", length = 255)
    private String createdByEmail;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public static String label(int major, int minor, int revision) {
        return major + "." + minor + "." + revision;
    }
}
