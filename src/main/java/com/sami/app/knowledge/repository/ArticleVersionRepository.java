package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.ArticleVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Long> {
    List<ArticleVersion> findAllByArticleIdOrderByVersionMajorDescVersionMinorDesc(Long articleId);
    Optional<ArticleVersion> findFirstByArticleIdAndIsCurrentTrue(Long articleId);
    Optional<ArticleVersion> findFirstByArticleIdAndIsPublishedTrue(Long articleId);
    Optional<ArticleVersion> findByArticleIdAndLabel(Long articleId, String label);
    long countByArticleId(Long articleId);
}
