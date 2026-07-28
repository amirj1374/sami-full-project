package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortalMessageRepository extends JpaRepository<PortalMessage, Long> {

    /** Customer-visible thread: internal staff notes are excluded. */
    List<PortalMessage> findAllByRequestIdAndIsInternalFalseOrderByCreatedAtAsc(Long requestId);

    List<PortalMessage> findAllByRequestIdOrderByCreatedAtAsc(Long requestId);

    Page<PortalMessage> findAllByAccountIdAndIsInternalFalseOrderByCreatedAtDesc(
            Long accountId, Pageable pageable);

    long countByAccountIdAndIsInternalFalseAndReadAtIsNull(Long accountId);
}
