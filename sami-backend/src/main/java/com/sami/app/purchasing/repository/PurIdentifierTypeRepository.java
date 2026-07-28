package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurIdentifierType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link PurIdentifierType} lookup. */
public interface PurIdentifierTypeRepository extends JpaRepository<PurIdentifierType, Long> {

    List<PurIdentifierType> findAllByOrderByDisplayOrderAsc();

    List<PurIdentifierType> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);
}
