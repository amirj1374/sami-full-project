package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommAuditLogRepository extends JpaRepository<CommAuditLog, Long> {
    Page<CommAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId, Pageable pageable);
}
