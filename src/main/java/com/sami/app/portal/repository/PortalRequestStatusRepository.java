package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalRequestStatusRepository extends JpaRepository<PortalRequestStatus, Long> {
    Optional<PortalRequestStatus> findByCode(String code);
    Optional<PortalRequestStatus> findFirstByIsDefaultTrue();
    Optional<PortalRequestStatus> findFirstByIsResolvedStateTrue();
    List<PortalRequestStatus> findAllByOrderByDisplayOrderAsc();
}
