package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupDuplicateRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the runtime-toggleable {@link SupDuplicateRule}s. */
public interface SupDuplicateRuleRepository extends JpaRepository<SupDuplicateRule, Long> {

    List<SupDuplicateRule> findByEnabledTrue();
}
