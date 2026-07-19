package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.LicenseAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseAuditLogRepository extends JpaRepository<LicenseAuditLog, Long> {
}
