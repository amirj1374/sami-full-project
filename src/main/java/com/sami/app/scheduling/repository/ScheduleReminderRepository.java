package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.ScheduleReminder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ScheduleReminderRepository extends JpaRepository<ScheduleReminder, Long> {
    @EntityGraph(attributePaths = {"channel"})
    List<ScheduleReminder> findByDeliveryStateAndScheduledForLessThanEqual(String state, Instant due);
    List<ScheduleReminder> findByScheduleId(Long scheduleId);
    void deleteByScheduleId(Long scheduleId);
}
