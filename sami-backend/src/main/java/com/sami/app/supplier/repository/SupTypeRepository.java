package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link SupType} lookup. */
public interface SupTypeRepository extends JpaRepository<SupType, Long> {

    List<SupType> findAllByOrderByDisplayOrderAsc();

    Optional<SupType> findByIsDefaultTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
