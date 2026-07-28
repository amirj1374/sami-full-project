package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortalNotificationRepository extends JpaRepository<PortalNotification, Long> {
    @EntityGraph(attributePaths = {"type"})
    Page<PortalNotification> findAllByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    @EntityGraph(attributePaths = {"type"})
    List<PortalNotification> findAllByAccountIdAndReadAtIsNullOrderByCreatedAtDesc(Long accountId);

    long countByAccountIdAndReadAtIsNull(Long accountId);
}
