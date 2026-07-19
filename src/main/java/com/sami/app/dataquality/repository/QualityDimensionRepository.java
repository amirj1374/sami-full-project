package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualityDimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QualityDimensionRepository extends JpaRepository<QualityDimension, Long> {

    List<QualityDimension> findAllByOrderByDisplayOrderAsc();

    Optional<QualityDimension> findByCode(String code);
}
