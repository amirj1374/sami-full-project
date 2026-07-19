package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalWidget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalWidgetRepository extends JpaRepository<PortalWidget, Long> {
    Optional<PortalWidget> findByCode(String code);
    List<PortalWidget> findAllByEnabledTrueOrderByDisplayOrderAsc();
    List<PortalWidget> findAllByOrderByDisplayOrderAsc();
}
