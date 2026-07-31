package com.sami.app.automation.repository;

import com.sami.app.automation.domain.AutomationExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

public interface AutomationExecutionRepository
        extends JpaRepository<AutomationExecution, Long>, JpaSpecificationExecutor<AutomationExecution> {

    Page<AutomationExecution> findByRuleIdAndTenantIdOrderByStartedAtDesc(Long ruleId, Long tenantId, Pageable pageable);

    java.util.Optional<AutomationExecution> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"rule"})
    Page<AutomationExecution> findByTenantIdOrderByStartedAtDesc(Long tenantId, Pageable pageable);

    long countByTenantIdAndStatus(Long tenantId, String status);

    long countByRuleIdAndStatus(Long ruleId, String status);

    boolean existsByRuleIdAndTenantId(Long ruleId, Long tenantId);

    /** Monotonic execution-number sequence (unique, concurrency-safe). */
    @Query(value = "SELECT nextval('automation_execution_seq')", nativeQuery = true)
    long nextNumber();
}
