package com.sami.app.automation.repository;

import com.sami.app.automation.domain.AutomationExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutomationExecutionLogRepository extends JpaRepository<AutomationExecutionLog, Long> {

    List<AutomationExecutionLog> findByExecutionIdOrderByStepOrderAsc(Long executionId);
}
