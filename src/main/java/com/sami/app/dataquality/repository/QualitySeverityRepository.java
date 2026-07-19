package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualitySeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QualitySeverityRepository extends JpaRepository<QualitySeverity, Long> {

    List<QualitySeverity> findAllByOrderByDisplayOrderAsc();

    Optional<QualitySeverity> findByCode(String code);
}
