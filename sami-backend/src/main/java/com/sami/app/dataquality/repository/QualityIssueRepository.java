package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualityIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface QualityIssueRepository
        extends JpaRepository<QualityIssue, Long>, JpaSpecificationExecutor<QualityIssue> {

    Page<QualityIssue> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<QualityIssue> findByModuleCodeAndEntityCodeAndEntityId(String moduleCode, String entityCode, Long entityId);

    long countByStatus(String status);

    long countByModuleCodeAndStatus(String moduleCode, String status);
}
