package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.ArticleView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArticleViewRepository extends JpaRepository<ArticleView, Long> {
    Page<ArticleView> findAllByArticleIdOrderByViewedAtDesc(Long articleId, Pageable pageable);

    /** Coverage: which business processes have readers looking for knowledge. */
    @Query("SELECT v.contextModule, v.contextEntity, count(v) FROM ArticleView v "
            + "WHERE v.contextModule IS NOT NULL GROUP BY v.contextModule, v.contextEntity")
    List<Object[]> countByContext();
}
