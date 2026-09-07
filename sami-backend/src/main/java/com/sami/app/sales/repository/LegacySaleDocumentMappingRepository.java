package com.sami.app.sales.repository;

import com.sami.app.sales.domain.LegacySaleDocumentMapping;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacySaleDocumentMappingRepository extends JpaRepository<LegacySaleDocumentMapping, Long> {

    List<LegacySaleDocumentMapping> findByTenantIdAndLegacySaleIdOrderByDocumentTypeAscDocumentIdAsc(
            Long tenantId, Long legacySaleId);

    boolean existsByTenantIdAndLegacySaleIdAndDocumentTypeAndDocumentId(
            Long tenantId, Long legacySaleId, String documentType, Long documentId);
}
