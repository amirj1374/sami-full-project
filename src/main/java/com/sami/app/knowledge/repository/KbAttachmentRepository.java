package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KbAttachmentRepository extends JpaRepository<KbAttachment, Long> {
    List<KbAttachment> findAllByArticleVersionIdOrderByDisplayOrderAsc(Long articleVersionId);
    List<KbAttachment> findAllByStepIdOrderByDisplayOrderAsc(Long stepId);
}
