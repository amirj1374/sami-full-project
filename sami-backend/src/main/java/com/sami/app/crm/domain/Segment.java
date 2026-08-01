package com.sami.app.crm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * A stored customer filter: marketing segment, saved search, or report input.
 * {@code filter} holds the listing filter shape as JSON, so any filter the
 * listing supports (including event-recency rules) can be persisted and re-run.
 */
@Entity
@Table(name = "crm_segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Segment extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> filter;
}
