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

import java.time.Instant;

/**
 * A recorded read. Captures the business context the reader came from, which is
 * what makes the knowledge-coverage and unused-article reports meaningful rather
 * than a bare hit counter.
 */
@Entity
@Table(name = "kb_article_views")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArticleView extends BaseEntity {

    @Column(name = "article_id", nullable = false) private Long articleId;
    @Column(name = "version_id") private Long versionId;
    @Column(name = "viewer_id") private Long viewerId;
    @Column(name = "viewer_email", length = 255) private String viewerEmail;
    @Column(name = "context_module", length = 64) private String contextModule;
    @Column(name = "context_entity", length = 64) private String contextEntity;
    @Column(name = "context_record_id") private Long contextRecordId;
    @Column(name = "viewed_at", nullable = false) @Builder.Default private Instant viewedAt = Instant.now();
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
