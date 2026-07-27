package com.sami.app.comm.repository;

import com.sami.app.comm.domain.CommMessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommMessageAttachmentRepository extends JpaRepository<CommMessageAttachment, Long> {
    List<CommMessageAttachment> findByMessageId(Long messageId);
}
