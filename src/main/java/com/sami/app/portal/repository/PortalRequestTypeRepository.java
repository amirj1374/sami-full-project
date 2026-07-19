package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalRequestTypeRepository extends JpaRepository<PortalRequestType, Long> {
    Optional<PortalRequestType> findByCode(String code);
    List<PortalRequestType> findAllByEnabledTrueOrderByDisplayOrderAsc();
    List<PortalRequestType> findAllByOrderByDisplayOrderAsc();
}
