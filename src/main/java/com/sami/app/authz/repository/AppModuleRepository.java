package com.sami.app.authz.repository;

import com.sami.app.authz.domain.AppModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data-access for {@link AppModule}.
 *
 * <p>Every read that builds a response touches the three lifecycle statuses, so
 * they are fetched eagerly here. Left lazy they would issue three extra queries
 * per module on each menu build — and the menu is loaded on every login and
 * route-guard pass.
 */
public interface AppModuleRepository extends JpaRepository<AppModule, Long> {

    boolean existsByCodeIgnoreCase(String code);

    /** Menu source: only enabled modules, in display order. */
    @EntityGraph(attributePaths = {"backendStatus", "frontendStatus", "overallStatus"})
    List<AppModule> findByEnabledTrueOrderByDisplayOrderAsc();

    /** Matrix source: every module (including disabled), in display order. */
    @EntityGraph(attributePaths = {"backendStatus", "frontendStatus", "overallStatus"})
    List<AppModule> findAllByOrderByDisplayOrderAsc();

    @Override
    @EntityGraph(attributePaths = {"backendStatus", "frontendStatus", "overallStatus"})
    Page<AppModule> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"backendStatus", "frontendStatus", "overallStatus"})
    Optional<AppModule> findById(Long id);
}
