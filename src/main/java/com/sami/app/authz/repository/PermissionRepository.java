package com.sami.app.authz.repository;

import com.sami.app.authz.domain.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data-access for {@link Permission}.
 *
 * <p>List queries fetch the owning module eagerly (entity graph) because every
 * permission DTO carries module code/name — avoids N+1 on listing.
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Override
    @EntityGraph(attributePaths = "module")
    Page<Permission> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "module")
    Page<Permission> findByModuleId(Long moduleId, Pageable pageable);

    List<Permission> findByModuleIdOrderByActionAsc(Long moduleId);

    /** Matrix source: every permission with its module, ready for grouping. */
    @EntityGraph(attributePaths = "module")
    List<Permission> findAllByOrderByActionAsc();

    boolean existsByModuleIdAndAction(Long moduleId, String action);

    long countByModuleId(Long moduleId);
}
