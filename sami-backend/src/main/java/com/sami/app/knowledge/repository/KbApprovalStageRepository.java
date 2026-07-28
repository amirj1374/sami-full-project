package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbApprovalStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbApprovalStageRepository extends JpaRepository<KbApprovalStage, Long> {
    Optional<KbApprovalStage> findByCode(String code);
    List<KbApprovalStage> findAllByOrderByStageOrderAsc();
    Optional<KbApprovalStage> findFirstByIsFinalTrue();
}
