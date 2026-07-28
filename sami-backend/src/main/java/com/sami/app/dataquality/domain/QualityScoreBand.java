package com.sami.app.dataquality.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Configurable score band (Excellent … Critical) matched by {@code minScore}. */
@Entity
@Table(name = "quality_score_bands")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QualityScoreBand extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal minScore;

    @Column(length = 32)
    private String color;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
