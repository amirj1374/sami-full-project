package com.sami.app.calendar.repository;

import com.sami.app.calendar.domain.BusinessCalendar;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessCalendarRepository extends JpaRepository<BusinessCalendar, Long> {

    // calendarSystem is needed on every resolution to pick the chronology, so
    // it is fetched eagerly here rather than risking a lazy load outside the
    // transaction — the failure mode that bit the files module in V18.
    @EntityGraph(attributePaths = {"calendarSystem"})
    Optional<BusinessCalendar> findByCode(String code);

    @EntityGraph(attributePaths = {"calendarSystem"})
    Optional<BusinessCalendar> findByIdAndIsActiveTrue(Long id);

    @EntityGraph(attributePaths = {"calendarSystem"})
    Optional<BusinessCalendar> findFirstByBranchIdAndIsActiveTrue(Long branchId);

    @EntityGraph(attributePaths = {"calendarSystem"})
    Optional<BusinessCalendar> findFirstByCompanyIdAndBranchIdIsNullAndIsActiveTrue(Long companyId);

    @EntityGraph(attributePaths = {"calendarSystem"})
    Optional<BusinessCalendar> findFirstByIsDefaultTrueAndIsActiveTrue();

    @EntityGraph(attributePaths = {"calendarSystem"})
    List<BusinessCalendar> findByIsActiveTrueOrderByDisplayOrderAsc();
}
