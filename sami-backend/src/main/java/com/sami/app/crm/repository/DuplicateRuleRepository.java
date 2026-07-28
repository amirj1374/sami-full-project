package com.sami.app.crm.repository;

import com.sami.app.crm.domain.DuplicateRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the runtime-toggleable {@link DuplicateRule} configuration. */
public interface DuplicateRuleRepository extends JpaRepository<DuplicateRule, Long> {

    List<DuplicateRule> findByEnabledTrue();
}
