package com.sami.app.automation.repository;

import com.sami.app.automation.domain.AutomationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AutomationStatusRepository extends JpaRepository<AutomationStatus, Long> {

    List<AutomationStatus> findAllByOrderByDisplayOrderAsc();

    Optional<AutomationStatus> findByCode(String code);

    Optional<AutomationStatus> findByIsDefaultTrue();
}
