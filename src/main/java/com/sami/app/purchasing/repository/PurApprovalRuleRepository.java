package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurApprovalRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link PurApprovalRule}s. */
public interface PurApprovalRuleRepository extends JpaRepository<PurApprovalRule, Long> {

    List<PurApprovalRule> findByActiveTrue();
}
