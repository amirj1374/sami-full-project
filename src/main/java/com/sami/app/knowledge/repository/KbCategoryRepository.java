package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.KbCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbCategoryRepository extends JpaRepository<KbCategory, Long> {
    @EntityGraph(attributePaths = {"parent"})
    Optional<KbCategory> findByCode(String code);

    @EntityGraph(attributePaths = {"parent"})
    List<KbCategory> findAllByOrderByDisplayOrderAsc();
}
