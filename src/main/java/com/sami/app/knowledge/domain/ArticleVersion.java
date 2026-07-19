package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An immutable snapshot of article content.
 *
 * <p>Publication swaps a pointer rather than overwriting content, so a procedure
 * someone followed last year can still be produced exactly as it stood — the
 * same guarantee the metadata module gives forms.
 */
@Entity
@Table(name = "kb_article_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArticleVersion extends BaseEntity {

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "version_major", nullable = false)
    @Builder.Default
    private int versionMajor = 1;

    @Column(name = "version_minor", nullable = false)
    @Builder.Default
    private int versionMinor = 0;

    /** Denormalised "1.0" for display and stable lookup. */
    @Column(nullable = false, length = 32)
    private String label;

    @Column(columnDefinition = "text")
    private String content;

    /** markdown | html | text */
    @Column(name = "content_format", nullable = false, length = 32)
    @Builder.Default
    private String contentFormat = "markdown";

    @Column(name = "change_note", length = 2000)
    private String changeNote;

    /** The editable draft. At most one per article. */
    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    /** The version readers see. At most one per article. */
    @Column(name = "is_published", nullable = false)
    private boolean isPublished;

    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "published_by") private Long publishedBy;
    @Column(name = "archived_at") private Instant archivedAt;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "created_by_email", length = 255) private String createdByEmail;

    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public static String label(int major, int minor) {
        return major + "." + minor;
    }
}
