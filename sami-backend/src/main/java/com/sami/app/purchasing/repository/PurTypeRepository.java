package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link PurType} lookup. */
public interface PurTypeRepository extends JpaRepository<PurType, Long> {

    List<PurType> findAllByOrderByDisplayOrderAsc();

    Optional<PurType> findByIsDefaultTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
