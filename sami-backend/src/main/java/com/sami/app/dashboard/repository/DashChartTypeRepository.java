package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashChartType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link DashChartType} lookup. */
public interface DashChartTypeRepository extends JpaRepository<DashChartType, Long> {

    List<DashChartType> findAllByOrderByDisplayOrderAsc();

    Optional<DashChartType> findByCodeIgnoreCase(String code);

    List<DashChartType> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
