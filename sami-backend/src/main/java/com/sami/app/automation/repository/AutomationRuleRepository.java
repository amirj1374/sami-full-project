package com.sami.app.automation.repository;

import com.sami.app.automation.domain.AutomationRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AutomationRuleRepository
        extends JpaRepository<AutomationRule, Long>, JpaSpecificationExecutor<AutomationRule> {

    boolean existsByTenantIdAndCode(Long tenantId, String code);

    long countByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"status", "actions"})
    Optional<AutomationRule> findWithActionsByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"status", "actions"})
    List<AutomationRule> findTop10000ByTenantIdOrderByPriorityAsc(Long tenantId);

    /**
     * All rules whose status is an active state, ordered by priority — the
     * candidate set the engine filters by trigger match on each dispatch.
     */
    @EntityGraph(attributePaths = {"status", "actions"})
    @Query("SELECT r FROM AutomationRule r JOIN r.status s WHERE r.tenantId = :tenantId AND s.isActiveState = true ORDER BY r.priority ASC")
    List<AutomationRule> findActiveRules(Long tenantId);

    @Override
    @EntityGraph(attributePaths = {"status"})
    org.springframework.data.domain.Page<AutomationRule> findAll(
            org.springframework.data.jpa.domain.Specification<AutomationRule> spec,
            org.springframework.data.domain.Pageable pageable);
}
