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

import java.util.ArrayList;
import java.util.List;

/**
 * The validation and processing contract for a kind of file. Adding a category
 * ("Certificate", "Digital Signature", …) is an INSERT, never a code change.
 *
 * <p>Empty {@code allowedMimeTypes}/{@code allowedExtensions} mean "any".
 */
@Entity
@Table(name = "file_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileCategory extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "max_bytes")
    private Long maxBytes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_mime_types", nullable = false)
    @Builder.Default
    private List<String> allowedMimeTypes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_extensions", nullable = false)
    @Builder.Default
    private List<String> allowedExtensions = new ArrayList<>();

    @Column(name = "scan_required", nullable = false)
    private boolean scanRequired;

    @Column(name = "versioning_enabled", nullable = false)
    private boolean versioningEnabled;

    @Column(name = "dedupe_enabled", nullable = false)
    private boolean dedupeEnabled;

    /** Ordered {@code file_processors.code} values applied on upload. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private List<String> processors = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retention_policy_id")
    private RetentionPolicy retentionPolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_provider_id")
    private StorageProviderConfig defaultProvider;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "tenant_id")
    private Long tenantId;
}
