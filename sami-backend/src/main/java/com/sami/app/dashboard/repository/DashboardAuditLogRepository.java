package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashboardAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data-access for the append-only {@link DashboardAuditLog}. */
public interface DashboardAuditLogRepository extends JpaRepository<DashboardAuditLog, Long> {

    Page<DashboardAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId, Pageable pageable);
}
