package com.sami.app.metadata.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Binds a business record to the form version it was captured with — the anchor
 * that keeps historical records compatible with old form versions.
 */
@Entity
@Table(name = "meta_record_form_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MetaRecordFormVersion extends BaseEntity {

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Column(name = "entity_code", nullable = false, length = 64)
    private String entityCode;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "form_version_id", nullable = false)
    private Long formVersionId;

    @Column(name = "captured_at", nullable = false)
    @Builder.Default
    private Instant capturedAt = Instant.now();
}
