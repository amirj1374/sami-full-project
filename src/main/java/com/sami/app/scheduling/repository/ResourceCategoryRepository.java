package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.ResourceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResourceCategoryRepository extends JpaRepository<ResourceCategory, Long> {
    Optional<ResourceCategory> findByCode(String code);
    List<ResourceCategory> findByIsActiveTrueOrderByDisplayOrderAsc();
}
