package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalAuditLogRepository extends JpaRepository<PortalAuditLog, Long> {
    Page<PortalAuditLog> findAllByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
    Page<PortalAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
