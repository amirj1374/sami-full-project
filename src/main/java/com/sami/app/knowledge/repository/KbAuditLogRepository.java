package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KbAuditLogRepository extends JpaRepository<KbAuditLog, Long> {
    Page<KbAuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId, Pageable pageable);
}
