package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortalAttachmentRepository extends JpaRepository<PortalAttachment, Long> {
    List<PortalAttachment> findAllByRequestId(Long requestId);
    List<PortalAttachment> findAllByMessageId(Long messageId);
}
