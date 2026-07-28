package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashKpiStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link DashKpiStatus} lookup. */
public interface DashKpiStatusRepository extends JpaRepository<DashKpiStatus, Long> {

    List<DashKpiStatus> findAllByOrderByDisplayOrderAsc();

    Optional<DashKpiStatus> findByIsDefaultTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
