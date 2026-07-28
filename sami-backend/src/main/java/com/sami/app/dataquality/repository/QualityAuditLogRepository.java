package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityAuditLogRepository extends JpaRepository<QualityAuditLog, Long> {
}
