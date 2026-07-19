package com.sami.app.files.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A generated rendition of one version: thumbnail, preview, OCR text,
 * watermarked copy. Keyed by version so a rollback exposes that version's
 * derivatives rather than the newest ones.
 */
@Entity
@Table(name = "file_derivatives")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileDerivative extends BaseEntity {

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(nullable = false, length = 64)
    private String kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private StorageProviderConfig provider;

    @Column(name = "storage_key", length = 1024)
    private String storageKey;

    @Column(name = "mime_type", length = 160)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    private Integer width;

    private Integer height;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "duration_ms")
    private Long durationMs;

    /** OCR output; null for binary derivatives. */
    @Column(name = "text_content", columnDefinition = "text")
    private String textContent;

    @Column(name = "generator_key", nullable = false, length = 64)
    private String generatorKey;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ready";

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
