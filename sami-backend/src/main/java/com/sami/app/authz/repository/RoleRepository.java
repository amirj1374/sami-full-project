package com.sami.app.authz.repository;

import com.sami.app.authz.domain.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Data-access for {@link Role}. */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /** The role assigned to newly registered users (at most one, DB-enforced). */
    Optional<Role> findByIsDefaultTrue();

    /** The super-admin role used to seed the bootstrap administrator. */
    Optional<Role> findFirstByIsSuperAdminTrue();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Loads a role with its permission set in a single query. */
    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(Long id);

    /**
     * Initializes the permission sets of an already-loaded page of roles in one
     * query — the returned entities are the same persistence-context instances,
     * so this avoids both a lazy query per role and the in-memory pagination a
     * fetching {@code findAll(Pageable)} would cause.
     */
    @EntityGraph(attributePaths = "permissions")
    List<Role> findWithPermissionsByIdIn(Collection<Long> ids);
}
