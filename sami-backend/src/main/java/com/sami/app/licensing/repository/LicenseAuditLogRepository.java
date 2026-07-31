package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.LicenseAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LicenseAuditLogRepository extends JpaRepository<LicenseAuditLog, Long> {

    List<LicenseAuditLog> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            Long tenantId, String entityType, Long entityId);
}
