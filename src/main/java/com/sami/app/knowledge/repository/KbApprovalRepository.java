package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbApproval;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbApprovalRepository extends JpaRepository<KbApproval, Long> {
    @EntityGraph(attributePaths = {"stage"})
    List<KbApproval> findAllByArticleVersionIdOrderByStageStageOrderAsc(Long articleVersionId);

    @EntityGraph(attributePaths = {"stage"})
    Optional<KbApproval> findByArticleVersionIdAndStageId(Long articleVersionId, Long stageId);

    void deleteAllByArticleVersionId(Long articleVersionId);
}
