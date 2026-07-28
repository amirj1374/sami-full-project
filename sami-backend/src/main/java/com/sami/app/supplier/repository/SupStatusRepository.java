package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link SupStatus} lookup. */
public interface SupStatusRepository extends JpaRepository<SupStatus, Long> {

    List<SupStatus> findAllByOrderByDisplayOrderAsc();

    Optional<SupStatus> findByIsDefaultTrue();

    Optional<SupStatus> findByIsArchivedStateTrue();

    Optional<SupStatus> findByIsDeletedStateTrue();

    Optional<SupStatus> findByIsBlacklistStateTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
