package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.FeatureState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureStateRepository extends JpaRepository<FeatureState, Long> {

    List<FeatureState> findAllByOrderByDisplayOrderAsc();

    Optional<FeatureState> findByCode(String code);

    Optional<FeatureState> findByIsDefaultTrue();
}
