package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link SupDocumentType} lookup. */
public interface SupDocumentTypeRepository extends JpaRepository<SupDocumentType, Long> {

    List<SupDocumentType> findAllByOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);
}
