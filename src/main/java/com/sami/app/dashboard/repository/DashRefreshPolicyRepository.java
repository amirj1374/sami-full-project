package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashRefreshPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link DashRefreshPolicy} lookup. */
public interface DashRefreshPolicyRepository extends JpaRepository<DashRefreshPolicy, Long> {

    List<DashRefreshPolicy> findAllByOrderByDisplayOrderAsc();

    Optional<DashRefreshPolicy> findByIsDefaultTrue();

    Optional<DashRefreshPolicy> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
