package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A link from an article to another article, or to any business record.
 *
 * <p>One table for both, because "related to this repair" and "related to this
 * article" are the same navigational need to a reader. A database constraint
 * enforces that exactly one target kind is set.
 */
@Entity
@Table(name = "kb_relations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbRelation extends BaseEntity {

    @Column(name = "article_id", nullable = false) private Long articleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relation_type_id", nullable = false)
    private KbRelationType relationType;

    @Column(name = "target_article_id") private Long targetArticleId;
    @Column(name = "target_module", length = 64) private String targetModule;
    @Column(name = "target_entity", length = 64) private String targetEntity;
    @Column(name = "target_record_id") private Long targetRecordId;
    @Column(length = 500) private String note;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
