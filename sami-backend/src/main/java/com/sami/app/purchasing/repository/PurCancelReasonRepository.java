package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurCancelReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link PurCancelReason} lookup. */
public interface PurCancelReasonRepository extends JpaRepository<PurCancelReason, Long> {

    List<PurCancelReason> findAllByOrderByDisplayOrderAsc();

    List<PurCancelReason> findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(Long tenantId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId, String code, Long id);
}
