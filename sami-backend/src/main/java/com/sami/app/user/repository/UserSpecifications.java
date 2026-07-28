package com.sami.app.user.repository;

import com.sami.app.user.domain.User;
import com.sami.app.user.domain.UserProfile;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable query fragments for {@link User} filtering.
 *
 * <p>Each method returns a {@link Specification} that is null-safe (a null argument
 * contributes no predicate), so callers can chain only the filters that were
 * actually supplied. Profile fields join {@code user_profiles} (LEFT, so users
 * without profile rows still match non-profile predicates).
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    /**
     * Global search: case-insensitive substring match on email, full name,
     * first/last name, phone number, employee code or national code.
     */
    public static Specification<User> searchContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> {
            Join<User, UserProfile> profile = profileJoin(root);
            return cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(profile.get("firstName")), pattern),
                    cb.like(cb.lower(profile.get("lastName")), pattern),
                    cb.like(cb.lower(profile.get("phoneNumber")), pattern),
                    cb.like(cb.lower(profile.get("employeeCode")), pattern),
                    cb.like(cb.lower(profile.get("nationalCode")), pattern));
        };
    }

    public static Specification<User> emailContains(String email) {
        return contains("email", email);
    }

    public static Specification<User> phoneContains(String phone) {
        return profileContains("phoneNumber", phone);
    }

    public static Specification<User> employeeCodeContains(String employeeCode) {
        return profileContains("employeeCode", employeeCode);
    }

    public static Specification<User> nationalCodeContains(String nationalCode) {
        return profileContains("nationalCode", nationalCode);
    }

    /** Users assigned the given role. Matches on the FK — no join needed. */
    public static Specification<User> hasRole(Long roleId) {
        if (roleId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("role").get("id"), roleId);
    }

    public static Specification<User> hasStatus(Long statusId) {
        if (statusId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status").get("id"), statusId);
    }

    /**
     * Default visibility rule: exclude users whose status is flagged
     * {@code hiddenByDefault} (archived / soft-deleted). Callers skip this
     * specification when the request opts into hidden users or targets a
     * specific status.
     */
    public static Specification<User> visibleByDefault() {
        return (root, query, cb) -> cb.isFalse(root.get("status").get("hiddenByDefault"));
    }

    private static Specification<User> contains(String attribute, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pattern = "%" + value.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(attribute)), pattern);
    }

    private static Specification<User> profileContains(String attribute, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pattern = "%" + value.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(profileJoin(root).get(attribute)), pattern);
    }

    @SuppressWarnings("unchecked")
    private static Join<User, UserProfile> profileJoin(Root<User> root) {
        // Reuse an existing join so multiple profile predicates share one LEFT JOIN.
        return (Join<User, UserProfile>) root.getJoins().stream()
                .filter(j -> "profile".equals(j.getAttribute().getName()))
                .findFirst()
                .orElseGet(() -> root.join("profile", JoinType.LEFT));
    }
}
