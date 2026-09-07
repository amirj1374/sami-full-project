package com.sami.app.sales.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Additive provenance between a preserved legacy Sale and a future separated
 * Sales document. The target is deliberately an id/type pair until the new
 * document tables exist; the legacy side remains a real foreign key.
 */
@Entity
@Table(name = "legacy_sale_document_mappings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegacySaleDocumentMapping extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legacy_sale_id", nullable = false)
    private Sale legacySale;

    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;

    @Column(name = "branch_id", nullable = false, updatable = false)
    private Long branchId;

    @Column(name = "document_type", nullable = false, length = 32, updatable = false)
    private String documentType;

    @Column(name = "document_id", nullable = false, updatable = false)
    private Long documentId;

    @Column(name = "mapping_reason", nullable = false, length = 48, updatable = false)
    private String mappingReason;
}
