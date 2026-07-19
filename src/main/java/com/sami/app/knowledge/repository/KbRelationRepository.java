package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbRelation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KbRelationRepository extends JpaRepository<KbRelation, Long> {
    @EntityGraph(attributePaths = {"relationType"})
    List<KbRelation> findAllByArticleIdOrderByDisplayOrderAsc(Long articleId);

    @EntityGraph(attributePaths = {"relationType"})
    List<KbRelation> findAllByTargetArticleId(Long targetArticleId);

    @EntityGraph(attributePaths = {"relationType"})
    List<KbRelation> findAllByTargetModuleAndTargetEntityAndTargetRecordId(
            String targetModule, String targetEntity, Long targetRecordId);

    long countByTargetArticleId(Long targetArticleId);
}
