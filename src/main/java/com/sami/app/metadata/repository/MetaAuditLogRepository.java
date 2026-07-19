package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaAuditLogRepository extends JpaRepository<MetaAuditLog, Long> {
}
