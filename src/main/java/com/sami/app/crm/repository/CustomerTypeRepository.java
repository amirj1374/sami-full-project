package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link CustomerType} lookup. */
public interface CustomerTypeRepository extends JpaRepository<CustomerType, Long> {

    List<CustomerType> findAllByOrderByDisplayOrderAsc();

    Optional<CustomerType> findByIsDefaultTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
