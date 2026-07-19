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
 * A configured storage target. {@code handlerKey} selects the
 * {@code StorageProviderHandler} bean; {@code config} is handler-specific.
 *
 * <p>Credentials are never stored here in plaintext — {@code config} holds a
 * reference resolved at runtime from environment or secret storage.
 */
@Entity
@Table(name = "storage_providers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StorageProviderConfig extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "handler_key", nullable = false, length = 64)
    private String handlerKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "supports_streaming", nullable = false)
    private boolean supportsStreaming;

    @Column(name = "supports_signed_url", nullable = false)
    private boolean supportsSignedUrl;

    @Column(name = "is_archive_tier", nullable = false)
    private boolean isArchiveTier;

    @Column(name = "max_file_bytes")
    private Long maxFileBytes;

    @Column(nullable = false)
    private int priority;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "tenant_id")
    private Long tenantId;
}
