package com.sami.app.crm.repository;

import com.sami.app.crm.domain.DuplicateRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data-access for the runtime-toggleable {@link DuplicateRule} configuration. */
public interface DuplicateRuleRepository extends JpaRepository<DuplicateRule, Long> {

    @Query("SELECT r FROM DuplicateRule r WHERE r.tenantId IS NULL OR r.tenantId = :tenantId ORDER BY r.id")
    List<DuplicateRule> findVisible(@Param("tenantId") Long tenantId);
}
