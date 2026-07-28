package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.ScheduleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ScheduleCategoryRepository extends JpaRepository<ScheduleCategory, Long> {
    Optional<ScheduleCategory> findByCode(String code);
    List<ScheduleCategory> findByIsActiveTrueOrderByDisplayOrderAsc();
}
