package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualityRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QualityRuleRepository
        extends JpaRepository<QualityRule, Long>, JpaSpecificationExecutor<QualityRule> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"status", "severity", "dimension"})
    Optional<QualityRule> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"status", "severity", "dimension"})
    List<QualityRule> findAllBy();

    /** Active rules targeting one entity, highest priority first. */
    @EntityGraph(attributePaths = {"status", "severity", "dimension"})
    @Query("SELECT r FROM QualityRule r JOIN r.status s "
            + "WHERE s.isActiveState = true AND r.moduleCode = :moduleCode AND r.entityCode = :entityCode "
            + "ORDER BY r.priority ASC")
    List<QualityRule> findActiveFor(String moduleCode, String entityCode);
}
