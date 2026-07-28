package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link PurStatus} lookup. */
public interface PurStatusRepository extends JpaRepository<PurStatus, Long> {

    List<PurStatus> findAllByOrderByDisplayOrderAsc();

    Optional<PurStatus> findByIsDraftStateTrue();

    Optional<PurStatus> findByIsPendingStateTrue();

    Optional<PurStatus> findByIsApprovedStateTrue();

    Optional<PurStatus> findByIsPartialStateTrue();

    Optional<PurStatus> findByIsCompletedStateTrue();

    Optional<PurStatus> findByIsCancelledStateTrue();

    Optional<PurStatus> findByIsRejectedStateTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
