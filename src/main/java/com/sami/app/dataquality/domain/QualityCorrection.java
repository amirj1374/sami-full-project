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

import java.time.Instant;

/**
 * A recorded correction for an issue. The module never writes to business
 * tables itself — it records what should change (or what a caller changed), so
 * ownership of business data stays with the owning module.
 */
@Entity
@Table(name = "quality_corrections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityCorrection extends BaseEntity {

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "field_name", length = 128)
    private String fieldName;

    @Column(name = "old_value", length = 2000)
    private String oldValue;

    @Column(name = "new_value", length = 2000)
    private String newValue;

    @Column(nullable = false)
    private boolean automatic;

    @Column(length = 1000)
    private String note;

    @Column(name = "applied_by")
    private Long appliedBy;

    @Column(name = "applied_by_email", length = 255)
    private String appliedByEmail;

    @Column(name = "applied_at", nullable = false)
    @Builder.Default
    private Instant appliedAt = Instant.now();
}
