package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
public interface PortalRequestRepository
        extends JpaRepository<PortalRequest, Long>, JpaSpecificationExecutor<PortalRequest> {

    @EntityGraph(attributePaths = {"type", "status"})
    Page<PortalRequest> findAllByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    @EntityGraph(attributePaths = {"type", "status"})
    List<PortalRequest> findAllByAccountIdOrderByCreatedAtDesc(Long accountId);

    @EntityGraph(attributePaths = {"type", "status"})
    Optional<PortalRequest> findByRequestNumber(String requestNumber);

    @Override
    @EntityGraph(attributePaths = {"type", "status"})
    Optional<PortalRequest> findById(Long id);

    /** Explicit JPQL: deriving through a boolean on a joined entity is fragile. */
    @Query("SELECT count(r) FROM PortalRequest r WHERE r.accountId = :accountId "
            + "AND r.status.isOpenState = TRUE")
    long countOpen(@org.springframework.data.repository.query.Param("accountId") Long accountId);

    @Query(value = "SELECT nextval('portal_request_seq')", nativeQuery = true)
    Long nextRequestSequence();

    /**
     * Specification queries do NOT inherit an {@code @EntityGraph} declared on
     * other methods, so this override is required: without it every association
     * loads lazily and the response mapper throws
     * {@code LazyInitializationException} once the session closes. This is the
     * list endpoint — the one a get-by-id test never exercises.
     */
    @Override
    @EntityGraph(attributePaths = {"type", "status"})
    Page<PortalRequest> findAll(Specification<PortalRequest> spec, Pageable pageable);
}
