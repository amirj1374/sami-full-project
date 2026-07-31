package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualityScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QualityScoreRepository extends JpaRepository<QualityScore, Long> {

    /** Most recent score snapshots for an entity — the trend series. */
    List<QualityScore> findTop30ByTenantIdAndModuleCodeAndEntityCodeOrderByComputedAtDesc(
            Long tenantId, String moduleCode, String entityCode);

    List<QualityScore> findTop100ByTenantIdOrderByComputedAtDesc(Long tenantId);
}
