package com.sami.app.knowledge.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.knowledge.domain.KbRelation;
import com.sami.app.knowledge.domain.KbRelationType;
import com.sami.app.knowledge.domain.KnowledgeArticle;
import com.sami.app.knowledge.repository.KbRelationRepository;
import com.sami.app.knowledge.repository.KbRelationTypeRepository;
import com.sami.app.knowledge.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Links between articles, and from articles to business records. */
@Service
@RequiredArgsConstructor
public class RelationService {

    private final KbRelationRepository relationRepository;
    private final KbRelationTypeRepository relationTypeRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final ArticleService articleService;
    private final RelationGraph relationGraph;
    private final KbAuditService auditService;

    @Transactional
    public KbRelation linkArticle(String articleCode, String relationTypeCode, String targetCode,
                                  String note) {
        KnowledgeArticle article = articleService.require(articleCode);
        KnowledgeArticle target = articleService.require(targetCode);
        KbRelationType type = requireType(relationTypeCode);

        if ("ENTITY".equals(type.getTargetKind())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Relation '%s' targets business records, not articles".formatted(relationTypeCode));
        }
        // Only directed relations can form a cycle; "related" is symmetric and exempt.
        if (!type.isSymmetric()) {
            relationGraph.assertNoCycle(directedEdges(type), article.getId(), target.getId());
        }

        KbRelation relation = relationRepository.save(KbRelation.builder()
                .articleId(article.getId())
                .relationType(type)
                .targetArticleId(target.getId())
                .note(note)
                .tenantId(article.getTenantId())
                .build());

        auditService.record("relation", relation.getId(), KbAuditService.CREATED, null,
                Map.of("from", articleCode, "to", targetCode, "type", relationTypeCode));
        return relation;
    }

    @Transactional
    public KbRelation linkEntity(String articleCode, String relationTypeCode, String module,
                                 String entity, Long recordId, String note) {
        KnowledgeArticle article = articleService.require(articleCode);
        KbRelationType type = requireType(relationTypeCode);
        if ("ARTICLE".equals(type.getTargetKind())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Relation '%s' targets articles, not business records".formatted(relationTypeCode));
        }
        return relationRepository.save(KbRelation.builder()
                .articleId(article.getId())
                .relationType(type)
                .targetModule(module)
                .targetEntity(entity)
                .targetRecordId(recordId)
                .note(note)
                .tenantId(article.getTenantId())
                .build());
    }

    @Transactional(readOnly = true)
    public List<KbRelation> forArticle(String articleCode) {
        return relationRepository.findAllByArticleIdOrderByDisplayOrderAsc(
                articleService.require(articleCode).getId());
    }

    /** Which knowledge applies to this business record? */
    @Transactional(readOnly = true)
    public List<KnowledgeArticle> forRecord(String module, String entity, Long recordId) {
        return relationRepository
                .findAllByTargetModuleAndTargetEntityAndTargetRecordId(module, entity, recordId)
                .stream()
                .map(r -> articleRepository.findById(r.getArticleId()).orElse(null))
                .filter(a -> a != null && a.getStatus().isVisible())
                .toList();
    }

    @Transactional
    public void delete(Long relationId) {
        KbRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Relation", relationId));
        auditService.record("relation", relationId, "Deleted",
                Map.of("articleId", relation.getArticleId()), null);
        relationRepository.delete(relation);
    }

    /** Existing directed edges of one relation type, for cycle detection. */
    private Map<Long, List<Long>> directedEdges(KbRelationType type) {
        Map<Long, List<Long>> edges = new HashMap<>();
        relationRepository.findAll().stream()
                .filter(r -> r.getTargetArticleId() != null)
                .filter(r -> r.getRelationType().getId().equals(type.getId()))
                .collect(Collectors.groupingBy(KbRelation::getArticleId))
                .forEach((from, list) -> edges.put(from,
                        list.stream().map(KbRelation::getTargetArticleId).toList()));
        return edges;
    }

    private KbRelationType requireType(String code) {
        return relationTypeRepository.findByCode(code)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unknown relation type: " + code));
    }
}
