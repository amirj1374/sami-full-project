package com.sami.app.automation.repository;

import com.sami.app.automation.domain.AutomationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationAuditLogRepository extends JpaRepository<AutomationAuditLog, Long> {
}
