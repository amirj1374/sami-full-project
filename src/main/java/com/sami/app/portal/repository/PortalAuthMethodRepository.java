package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalAuthMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalAuthMethodRepository extends JpaRepository<PortalAuthMethod, Long> {
    Optional<PortalAuthMethod> findByCode(String code);
    List<PortalAuthMethod> findAllByEnabledTrueOrderByDisplayOrderAsc();
    List<PortalAuthMethod> findAllByOrderByDisplayOrderAsc();
}
