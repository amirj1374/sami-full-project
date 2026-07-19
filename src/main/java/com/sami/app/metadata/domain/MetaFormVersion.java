package com.sami.app.metadata.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

/**
 * An immutable published snapshot of a form's schema. Publishing freezes the
 * schema so records captured against it stay renderable after the form evolves.
 */
@Entity
@Table(name = "meta_form_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaFormVersion extends BaseEntity {

    public enum Status { DRAFT, PUBLISHED, ARCHIVED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private MetaForm form;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = Status.DRAFT.name();

    /** Sections + field references + ordering. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false)
    @Builder.Default
    private Map<String, Object> schemaJson = new HashMap<>();

    @Column(name = "change_note", length = 1000)
    private String changeNote;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private Long publishedBy;
}
