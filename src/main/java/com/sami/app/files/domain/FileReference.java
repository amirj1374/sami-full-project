package com.sami.app.files.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Records which business record points at a file.
 *
 * <p>This is what makes deletion safe: a file with live references cannot be
 * purged, and orphan detection becomes a query rather than a guess. It also
 * carries access control — permission to read a file is derived from permission
 * on the referencing record, not from a blanket {@code files:view}.
 */
@Entity
@Table(name = "file_references")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileReference extends BaseEntity {

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Column(name = "entity_code", nullable = false, length = 64)
    private String entityCode;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    /** Distinguishes several files on one record (avatar, signature, photo-3). */
    @Column(length = 64)
    private String role;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
