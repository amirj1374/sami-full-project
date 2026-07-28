package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
public interface PortalAccountRepository
        extends JpaRepository<PortalAccount, Long>, JpaSpecificationExecutor<PortalAccount> {

    /** Status is dereferenced on every login and every response; always fetched. */
    @EntityGraph(attributePaths = {"status"})
    Optional<PortalAccount> findByUsername(String username);

    @EntityGraph(attributePaths = {"status"})
    Optional<PortalAccount> findByMobileNumber(String mobileNumber);

    @EntityGraph(attributePaths = {"status"})
    Optional<PortalAccount> findByCustomerId(Long customerId);

    @Override
    @EntityGraph(attributePaths = {"status"})
    Optional<PortalAccount> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"status"})
    Page<PortalAccount> findAll(Pageable pageable);

    boolean existsByCustomerId(Long customerId);

    /**
     * Specification queries do NOT inherit an {@code @EntityGraph} declared on
     * other methods, so this override is required: without it every association
     * loads lazily and the response mapper throws
     * {@code LazyInitializationException} once the session closes. This is the
     * list endpoint — the one a get-by-id test never exercises.
     */
    @Override
    @EntityGraph(attributePaths = {"status"})
    Page<PortalAccount> findAll(Specification<PortalAccount> spec, Pageable pageable);
}
