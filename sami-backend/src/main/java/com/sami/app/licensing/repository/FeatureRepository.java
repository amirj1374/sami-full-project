package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.Feature;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"dependencies", "state"})
    Optional<Feature> findByCode(String code);

    @EntityGraph(attributePaths = {"dependencies", "state"})
    List<Feature> findAllByOrderByDisplayOrderAsc();

    List<Feature> findByModuleCodeOrderByDisplayOrderAsc(String moduleCode);
}
