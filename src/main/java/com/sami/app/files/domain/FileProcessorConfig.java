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

/** A configured pipeline step; {@code handlerKey} selects the {@code FileProcessor} bean. */
@Entity
@Table(name = "file_processors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileProcessorConfig extends BaseEntity {

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

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "is_async", nullable = false)
    private boolean isAsync;

    /** When true a processor error aborts the upload instead of being logged. */
    @Column(name = "fails_upload", nullable = false)
    private boolean failsUpload;

    @Column(name = "run_order", nullable = false)
    private int runOrder;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "tenant_id")
    private Long tenantId;
}
