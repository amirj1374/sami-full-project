package com.sami.app.calendar.repository;

import com.sami.app.calendar.domain.CalendarSystemDef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarSystemDefRepository extends JpaRepository<CalendarSystemDef, Long> {
    Optional<CalendarSystemDef> findByCode(String code);
    Optional<CalendarSystemDef> findFirstByIsDefaultTrue();
    List<CalendarSystemDef> findByIsActiveTrueOrderByDisplayOrderAsc();
}
