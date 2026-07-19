package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.KpiValue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the historical {@link KpiValue} store. */
public interface KpiValueRepository extends JpaRepository<KpiValue, Long> {

    Optional<KpiValue> findTopByKpiIdOrderByComputedAtDesc(Long kpiId);

    List<KpiValue> findByKpiIdOrderByComputedAtDesc(Long kpiId, Pageable pageable);
}
