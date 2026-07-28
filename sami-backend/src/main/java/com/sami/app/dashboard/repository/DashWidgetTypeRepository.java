package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashWidgetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link DashWidgetType} lookup. */
public interface DashWidgetTypeRepository extends JpaRepository<DashWidgetType, Long> {

    List<DashWidgetType> findAllByOrderByDisplayOrderAsc();

    Optional<DashWidgetType> findByCodeIgnoreCase(String code);

    List<DashWidgetType> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
