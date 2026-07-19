package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * An attachment on an article version or a specific step.
 *
 * <p>Holds a {@code fileUuid} REFERENCE into the files module, or an external
 * URL — never bytes. This module is not a file store.
 */
@Entity
@Table(name = "kb_attachments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbAttachment extends BaseEntity {

    @Column(name = "article_version_id", nullable = false) private Long articleVersionId;
    @Column(name = "step_id") private Long stepId;

    /** Reference into the files module. Mutually exclusive with externalUrl. */
    @Column(name = "file_uuid") private UUID fileUuid;

    @Column(name = "external_url", length = 2000) private String externalUrl;

    /** image | video | document | spreadsheet | link | template */
    @Column(nullable = false, length = 32) @Builder.Default private String kind = "document";

    @Column(length = 500) private String caption;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
