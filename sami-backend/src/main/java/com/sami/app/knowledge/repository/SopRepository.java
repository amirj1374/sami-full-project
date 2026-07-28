package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.Sop;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SopRepository extends JpaRepository<Sop, Long> {
    @EntityGraph(attributePaths = {"riskLevel"})
    Optional<Sop> findByArticleVersionId(Long articleVersionId);

    @Override
    @EntityGraph(attributePaths = {"riskLevel"})
    Optional<Sop> findById(Long id);
}
