package com.sami.app.files.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks an in-progress upload so an interrupted transfer is recoverable rather
 * than leaking a half-written object. Sessions that never complete are reclaimed
 * by the retention sweep.
 */
@Entity
@Table(name = "file_upload_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileUploadSession extends BaseEntity {

    @Column(name = "session_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID sessionUuid = UUID.randomUUID();

    @Column(name = "file_id")
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private FileCategory category;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "declared_size")
    private Long declaredSize;

    @Column(name = "declared_checksum", length = 64)
    private String declaredChecksum;

    @Column(name = "received_bytes", nullable = false)
    @Builder.Default
    private long receivedBytes = 0L;

    @Column(name = "chunk_count", nullable = false)
    @Builder.Default
    private int chunkCount = 0;

    /** open | completed | aborted | expired | failed */
    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "open";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private StorageProviderConfig provider;

    @Column(name = "staging_key", length = 1024)
    private String stagingKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
