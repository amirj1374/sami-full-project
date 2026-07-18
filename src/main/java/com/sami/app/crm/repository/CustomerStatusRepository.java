package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link CustomerStatus} lookup. */
public interface CustomerStatusRepository extends JpaRepository<CustomerStatus, Long> {

    List<CustomerStatus> findAllByOrderByDisplayOrderAsc();

    Optional<CustomerStatus> findByIsDefaultTrue();

    Optional<CustomerStatus> findByIsArchivedStateTrue();

    Optional<CustomerStatus> findByIsDeletedStateTrue();

    Optional<CustomerStatus> findByIsBlacklistStateTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
