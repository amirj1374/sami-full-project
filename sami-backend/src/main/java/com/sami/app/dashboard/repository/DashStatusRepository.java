package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link DashStatus} lookup. */
public interface DashStatusRepository extends JpaRepository<DashStatus, Long> {

    List<DashStatus> findAllByOrderByDisplayOrderAsc();

    Optional<DashStatus> findByIsDefaultTrue();

    Optional<DashStatus> findByIsArchivedStateTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
