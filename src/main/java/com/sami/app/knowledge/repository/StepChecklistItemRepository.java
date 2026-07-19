package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.StepChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StepChecklistItemRepository extends JpaRepository<StepChecklistItem, Long> {
    List<StepChecklistItem> findAllByStepIdOrderByDisplayOrderAsc(Long stepId);
    void deleteAllByStepId(Long stepId);
}
