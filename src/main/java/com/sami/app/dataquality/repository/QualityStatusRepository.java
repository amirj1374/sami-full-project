package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QualityStatusRepository extends JpaRepository<QualityStatus, Long> {

    List<QualityStatus> findAllByOrderByDisplayOrderAsc();

    Optional<QualityStatus> findByCode(String code);
}
