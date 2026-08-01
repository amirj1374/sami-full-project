package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurIdentifierType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link PurIdentifierType} lookup. */
public interface PurIdentifierTypeRepository extends JpaRepository<PurIdentifierType, Long> {

    List<PurIdentifierType> findAllByOrderByDisplayOrderAsc();

    List<PurIdentifierType> findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(Long tenantId);

    List<PurIdentifierType> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId, String code, Long id);
}
