package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbStepType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbStepTypeRepository extends JpaRepository<KbStepType, Long> {
    Optional<KbStepType> findByCode(String code);
    Optional<KbStepType> findFirstByIsDefaultTrue();
    List<KbStepType> findAllByOrderByDisplayOrderAsc();
}
