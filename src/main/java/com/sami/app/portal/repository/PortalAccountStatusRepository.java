package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalAccountStatusRepository extends JpaRepository<PortalAccountStatus, Long> {
    Optional<PortalAccountStatus> findByCode(String code);
    Optional<PortalAccountStatus> findFirstByIsDefaultTrue();
    Optional<PortalAccountStatus> findFirstByAllowsLoginTrue();
    Optional<PortalAccountStatus> findFirstByIsLockedStateTrue();
    Optional<PortalAccountStatus> findFirstByIsSuspendedStateTrue();
    List<PortalAccountStatus> findAllByOrderByDisplayOrderAsc();
}
