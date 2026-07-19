package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalCapability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalCapabilityRepository extends JpaRepository<PortalCapability, Long> {
    Optional<PortalCapability> findByCode(String code);
    List<PortalCapability> findAllByGrantedByDefaultTrue();
    List<PortalCapability> findAllByOrderByDisplayOrderAsc();
    List<PortalCapability> findAllByIdIn(List<Long> ids);
}
