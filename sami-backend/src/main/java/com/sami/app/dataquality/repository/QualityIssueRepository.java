package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualityIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface QualityIssueRepository
        extends JpaRepository<QualityIssue, Long>, JpaSpecificationExecutor<QualityIssue> {

    Page<QualityIssue> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, String status, Pageable pageable);

    List<QualityIssue> findByTenantIdAndModuleCodeAndEntityCodeAndEntityId(Long tenantId, String moduleCode, String entityCode, Long entityId);

    long countByTenantIdAndStatus(Long tenantId, String status);

    long countByTenantIdAndModuleCodeAndStatus(Long tenantId, String moduleCode, String status);

    java.util.Optional<QualityIssue> findByIdAndTenantId(Long id, Long tenantId);
}
