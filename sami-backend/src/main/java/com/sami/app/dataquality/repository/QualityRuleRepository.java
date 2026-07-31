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

    boolean existsByTenantIdAndCode(Long tenantId, String code);

    @EntityGraph(attributePaths = {"status", "severity", "dimension"})
    @Query("SELECT r FROM QualityRule r LEFT JOIN FETCH r.status LEFT JOIN FETCH r.severity LEFT JOIN FETCH r.dimension WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<QualityRule> findWithDetailsByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"status", "severity", "dimension"})
    @Query("SELECT r FROM QualityRule r LEFT JOIN FETCH r.status LEFT JOIN FETCH r.severity LEFT JOIN FETCH r.dimension WHERE r.tenantId = :tenantId OR r.tenantId IS NULL ORDER BY r.priority")
    List<QualityRule> findAllVisible(Long tenantId);

    /** Active rules targeting one entity, highest priority first. */
    @EntityGraph(attributePaths = {"status", "severity", "dimension"})
    @Query("SELECT r FROM QualityRule r JOIN r.status s "
            + "WHERE s.isActiveState = true AND (r.tenantId = :tenantId OR r.tenantId IS NULL) AND r.moduleCode = :moduleCode AND r.entityCode = :entityCode "
            + "ORDER BY r.priority ASC")
    List<QualityRule> findActiveFor(Long tenantId, String moduleCode, String entityCode);
}
