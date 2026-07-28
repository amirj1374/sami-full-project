package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.SopStep;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SopStepRepository extends JpaRepository<SopStep, Long> {
    @EntityGraph(attributePaths = {"stepType"})
    List<SopStep> findAllBySopIdOrderByStepNumberAsc(Long sopId);

    void deleteAllBySopId(Long sopId);
    long countBySopId(Long sopId);
}
