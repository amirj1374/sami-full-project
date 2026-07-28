package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbRiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbRiskLevelRepository extends JpaRepository<KbRiskLevel, Long> {
    Optional<KbRiskLevel> findByCode(String code);
    Optional<KbRiskLevel> findFirstByIsDefaultTrue();
    List<KbRiskLevel> findAllByOrderByDisplayOrderAsc();
}
