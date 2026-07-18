package com.sami.app.user.repository;

import com.sami.app.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * Data-access for {@link User}.
 *
 * <p>{@link JpaSpecificationExecutor} enables the admin listing's dynamic filters
 * (search, role, enabled) without hand-writing query permutations.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Loads the user together with its role and the role's permissions in one
     * query — this runs on every authenticated request (JWT filter), so the
     * entity graph keeps authority loading to a single round-trip.
     */
    @EntityGraph(attributePaths = {"role", "role.permissions", "status"})
    Optional<User> findByEmail(String email);

    /**
     * Loads a user by id with role, permissions and status in a single query —
     * used to build the {@code /me} payload, which ships the full permission list.
     */
    @EntityGraph(attributePaths = {"role", "role.permissions", "status"})
    Optional<User> findWithRoleById(Long id);

    /** Full detail load for admin views: role, status and profile in one query. */
    @EntityGraph(attributePaths = {"role", "status", "profile"})
    Optional<User> findWithDetailsById(Long id);

    /** Listing override: fetch role and status with the page (no N+1 while mapping). */
    @Override
    @EntityGraph(attributePaths = {"role", "status"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    /** Export override: profile included, since CSV rows contain profile fields. */
    @Override
    @EntityGraph(attributePaths = {"role", "status", "profile"})
    List<User> findAll(Specification<User> spec, Sort sort);

    boolean existsByEmail(String email);

    long countByRoleId(Long roleId);

    List<User> findByRoleId(Long roleId);

    long countByStatusId(Long statusId);
}
