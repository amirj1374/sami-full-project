package com.sami.app.dataquality.repository;

import com.sami.app.dataquality.domain.QualityScoreBand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QualityScoreBandRepository extends JpaRepository<QualityScoreBand, Long> {

    /** Bands ordered from the highest threshold down, for band resolution. */
    List<QualityScoreBand> findAllByOrderByMinScoreDesc();

    List<QualityScoreBand> findAllByOrderByDisplayOrderAsc();
}
