package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbRelationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbRelationTypeRepository extends JpaRepository<KbRelationType, Long> {
    Optional<KbRelationType> findByCode(String code);
    List<KbRelationType> findAllByOrderByDisplayOrderAsc();
}
