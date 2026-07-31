package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualityCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QualityCorrectionRepository extends JpaRepository<QualityCorrection, Long> {

    List<QualityCorrection> findByIssueIdAndTenantIdOrderByAppliedAtDesc(Long issueId, Long tenantId);
}
