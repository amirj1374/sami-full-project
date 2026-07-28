package com.sami.app.authz.repository;

import com.sami.app.authz.domain.ModuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable module lifecycle stages. */
public interface ModuleStatusRepository extends JpaRepository<ModuleStatus, Long> {

    Optional<ModuleStatus> findByCode(String code);

    /** Seed status for a newly created module's backend axis. */
    Optional<ModuleStatus> findFirstByIsDefaultBackendTrue();

    /** Seed status for a newly created module's frontend axis. */
    Optional<ModuleStatus> findFirstByIsDefaultFrontendTrue();

    List<ModuleStatus> findAllByOrderByDisplayOrderAsc();

    /** Stages valid for the backend axis — drives the admin dropdown. */
    List<ModuleStatus> findByAppliesToBackendTrueOrderByDisplayOrderAsc();

    /** Stages valid for the frontend axis. */
    List<ModuleStatus> findByAppliesToFrontendTrueOrderByDisplayOrderAsc();
}
