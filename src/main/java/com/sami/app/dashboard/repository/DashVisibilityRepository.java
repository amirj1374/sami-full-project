package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link DashVisibility} lookup. */
public interface DashVisibilityRepository extends JpaRepository<DashVisibility, Long> {

    List<DashVisibility> findAllByOrderByDisplayOrderAsc();

    Optional<DashVisibility> findByIsDefaultTrue();

    Optional<DashVisibility> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
