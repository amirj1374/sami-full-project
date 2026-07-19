package com.sami.app.calendar.repository;

import com.sami.app.calendar.domain.HolidayType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HolidayTypeRepository extends JpaRepository<HolidayType, Long> {
    Optional<HolidayType> findByCode(String code);
    Optional<HolidayType> findFirstByIsDefaultTrue();
    List<HolidayType> findAllByOrderByDisplayOrderAsc();
}
