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

/** Outcome of one virus/malware scan of one version. */
@Entity
@Table(name = "file_scan_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileScanResult extends BaseEntity {

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "scanner_key", nullable = false, length = 64)
    private String scannerKey;

    /** clean | infected | suspicious | error | skipped */
    @Column(nullable = false, length = 32)
    private String verdict;

    @Column(length = 255)
    private String threat;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    @Column(name = "scanned_at", nullable = false)
    @Builder.Default
    private Instant scannedAt = Instant.now();

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
