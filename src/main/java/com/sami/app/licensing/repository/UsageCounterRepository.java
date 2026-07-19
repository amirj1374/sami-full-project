package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, Long> {

    Optional<UsageCounter> findByTenantIdAndLimitTypeIdAndPeriodKey(Long tenantId, Long limitTypeId, String periodKey);

    List<UsageCounter> findByTenantId(Long tenantId);
}
