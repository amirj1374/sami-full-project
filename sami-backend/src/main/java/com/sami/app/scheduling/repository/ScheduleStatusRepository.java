package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ScheduleStatusRepository extends JpaRepository<ScheduleStatus, Long> {
    Optional<ScheduleStatus> findByCode(String code);
    Optional<ScheduleStatus> findFirstByIsDefaultTrue();
    Optional<ScheduleStatus> findFirstByIsCancelledStateTrue();
    Optional<ScheduleStatus> findFirstByIsNoShowStateTrue();
    Optional<ScheduleStatus> findFirstByIsCheckedInStateTrue();
    Optional<ScheduleStatus> findFirstByIsCompletedStateTrue();
    Optional<ScheduleStatus> findFirstByIsConfirmedStateTrue();
    List<ScheduleStatus> findAllByOrderByDisplayOrderAsc();
}
