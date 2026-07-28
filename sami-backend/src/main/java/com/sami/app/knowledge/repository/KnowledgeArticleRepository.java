package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KnowledgeArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
public interface KnowledgeArticleRepository
        extends JpaRepository<KnowledgeArticle, Long>, JpaSpecificationExecutor<KnowledgeArticle> {

    /**
     * Category and status are dereferenced by every response mapper, so every
     * read path fetches them eagerly — a lazy proxy there is the exact defect
     * that shipped broken in an earlier module because only get-by-id was tested.
     */
    @EntityGraph(attributePaths = {"category", "status"})
    Optional<KnowledgeArticle> findByArticleCode(String articleCode);

    @Override
    @EntityGraph(attributePaths = {"category", "status"})
    Optional<KnowledgeArticle> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "status"})
    List<KnowledgeArticle> findAll();

    /** Context-sensitive lookup: what should this screen show the operator? */
    @EntityGraph(attributePaths = {"category", "status"})
    List<KnowledgeArticle> findAllByModuleCodeAndProcessCodeAndStatusIsVisibleTrueOrderByTitleAsc(
            String moduleCode, String processCode);

    @EntityGraph(attributePaths = {"category", "status"})
    List<KnowledgeArticle> findAllByModuleCodeAndStatusIsVisibleTrueOrderByTitleAsc(String moduleCode);

    /**
     * Full-text search against the trigger-maintained vector. The configuration
     * is chosen per language, matching how the vector was built — querying with a
     * different configuration than the one that produced the vector silently
     * returns nothing.
     */
    @Query(value = """
            SELECT * FROM kb_articles a
            WHERE a.search_vector @@ websearch_to_tsquery(
                    CASE WHEN a.language = 'en' THEN 'english'::regconfig ELSE 'simple'::regconfig END,
                    :term)
              AND (:onlyVisible = FALSE OR a.status_id IN (SELECT id FROM kb_statuses WHERE is_visible))
            ORDER BY ts_rank(a.search_vector, websearch_to_tsquery(
                    CASE WHEN a.language = 'en' THEN 'english'::regconfig ELSE 'simple'::regconfig END,
                    :term)) DESC
            """,
            countQuery = """
            SELECT count(*) FROM kb_articles a
            WHERE a.search_vector @@ websearch_to_tsquery(
                    CASE WHEN a.language = 'en' THEN 'english'::regconfig ELSE 'simple'::regconfig END,
                    :term)
              AND (:onlyVisible = FALSE OR a.status_id IN (SELECT id FROM kb_statuses WHERE is_visible))
            """,
            nativeQuery = true)
    Page<KnowledgeArticle> search(@Param("term") String term,
                                  @Param("onlyVisible") boolean onlyVisible,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {"category", "status"})
    List<KnowledgeArticle> findAllByReviewDateLessThanEqualAndArchivedAtIsNull(LocalDate cutoff);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.viewCount = 0 AND a.archivedAt IS NULL")
    List<KnowledgeArticle> findUnused();

    @Query(value = "SELECT nextval('kb_article_seq')", nativeQuery = true)
    Long nextArticleSequence();

    @Query(value = "SELECT nextval('kb_sop_seq')", nativeQuery = true)
    Long nextSopSequence();

    /**
     * Specification queries do NOT inherit an {@code @EntityGraph} declared on
     * other methods, so this override is required: without it every association
     * loads lazily and the response mapper throws
     * {@code LazyInitializationException} once the session closes. This is the
     * list endpoint — the one a get-by-id test never exercises.
     */
    @Override
    @EntityGraph(attributePaths = {"category", "status"})
    Page<KnowledgeArticle> findAll(Specification<KnowledgeArticle> spec, Pageable pageable);
}
