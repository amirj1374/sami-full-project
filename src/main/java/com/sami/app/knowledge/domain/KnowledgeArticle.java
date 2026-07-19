package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A knowledge asset: SOP, policy, work instruction, troubleshooting guide, FAQ.
 *
 * <p>The article is the stable identity; its CONTENT lives on versions. Business
 * modules reference an article by {@code articleCode} or by module/process
 * binding and never store operational knowledge themselves.
 *
 * <p>{@code searchVector} is maintained by a database trigger, not by Java, so
 * search stays correct no matter which code path writes the row.
 */
@Entity
@Table(name = "kb_articles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KnowledgeArticle extends BaseEntity {

    @Column(name = "article_code", nullable = false, length = 32)
    private String articleCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String summary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private KbCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private KbStatus status;

    /** Business module this knowledge belongs to, e.g. {@code repair}. */
    @Column(name = "module_code", length = 64)
    private String moduleCode;

    /** Business process within the module, enabling context-sensitive display. */
    @Column(name = "process_code", length = 64)
    private String processCode;

    @Column(name = "company_id") private Long companyId;
    @Column(name = "branch_id") private Long branchId;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String language = "en";

    @Column(name = "owner_id") private Long ownerId;
    @Column(name = "owner_email", length = 255) private String ownerEmail;
    @Column(name = "reviewer_id") private Long reviewerId;

    /** The draft being worked on. */
    @Column(name = "current_version_id") private Long currentVersionId;

    /** The version readers see. Null until first publication. */
    @Column(name = "published_version_id") private Long publishedVersionId;

    @Column(length = 1000) private String keywords;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private long viewCount = 0L;

    @Column(name = "last_viewed_at") private Instant lastViewedAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "effective_date") private LocalDate effectiveDate;
    @Column(name = "review_date") private LocalDate reviewDate;
    @Column(name = "archived_at") private Instant archivedAt;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "created_by_email", length = 255) private String createdByEmail;

    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean isPublished() {
        return publishedVersionId != null && status != null && status.isPublishedState();
    }

    public boolean isReviewOverdue() {
        return reviewDate != null && reviewDate.isBefore(LocalDate.now());
    }
}
