package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashDataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link DashDataSource} lookup. */
public interface DashDataSourceRepository extends JpaRepository<DashDataSource, Long> {

    List<DashDataSource> findAllByOrderByDisplayOrderAsc();

    Optional<DashDataSource> findByCodeIgnoreCase(String code);

    List<DashDataSource> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
