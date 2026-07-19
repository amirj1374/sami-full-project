package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.ScheduleAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleAuditLogRepository extends JpaRepository<ScheduleAuditLog, Long> {
    Page<ScheduleAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId, Pageable pageable);
}
