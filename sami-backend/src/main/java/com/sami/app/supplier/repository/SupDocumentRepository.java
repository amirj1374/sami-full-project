package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link SupDocument}. */
public interface SupDocumentRepository extends JpaRepository<SupDocument, Long> {

    @EntityGraph(attributePaths = {"docType"})
    List<SupDocument> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
}
