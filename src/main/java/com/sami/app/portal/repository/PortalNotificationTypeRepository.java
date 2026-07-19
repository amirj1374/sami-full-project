package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalNotificationTypeRepository extends JpaRepository<PortalNotificationType, Long> {
    Optional<PortalNotificationType> findByCode(String code);
    List<PortalNotificationType> findAllByOrderByDisplayOrderAsc();
}
