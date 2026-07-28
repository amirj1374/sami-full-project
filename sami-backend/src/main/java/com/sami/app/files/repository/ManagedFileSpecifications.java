package com.sami.app.files.repository;

import com.sami.app.files.domain.ManagedFile;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

/**
 * Composable search predicates. Every filter is null-tolerant so the controller
 * can combine any subset without branching — this is what "unlimited combined
 * filters" resolves to in the existing Specification convention.
 */
public final class ManagedFileSpecifications {

    private ManagedFileSpecifications() {
    }

    public static Specification<ManagedFile> nameContains(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String pattern = "%" + term.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("originalFilename")), pattern));
    }

    public static Specification<ManagedFile> categoryCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("code"), code);
    }

    public static Specification<ManagedFile> statusCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status").get("code"), code);
    }

    public static Specification<ManagedFile> module(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("moduleCode"), moduleCode);
    }

    public static Specification<ManagedFile> entity(String entityCode, Long entityId) {
        if (entityCode == null || entityCode.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get("entityCode"), entityCode);
            return entityId == null ? p : cb.and(p, cb.equal(root.get("entityId"), entityId));
        };
    }

    public static Specification<ManagedFile> owner(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    public static Specification<ManagedFile> folder(Long folderId) {
        if (folderId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("folder").get("id"), folderId);
    }

    public static Specification<ManagedFile> company(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<ManagedFile> branch(Long branchId) {
        if (branchId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("branchId"), branchId);
    }

    public static Specification<ManagedFile> idIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("id").in(ids);
    }

    public static Specification<ManagedFile> sizeBetween(Long min, Long max) {
        if (min == null && max == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("sizeBytes"), min, max);
            }
            return min != null
                    ? cb.greaterThanOrEqualTo(root.get("sizeBytes"), min)
                    : cb.lessThanOrEqualTo(root.get("sizeBytes"), max);
        };
    }

    public static Specification<ManagedFile> uploadedBetween(Instant from, Instant to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            return from != null
                    ? cb.greaterThanOrEqualTo(root.get("createdAt"), from)
                    : cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    /** Soft-deleted files are excluded unless explicitly requested. */
    public static Specification<ManagedFile> deleted(boolean includeDeleted) {
        if (includeDeleted) {
            return null;
        }
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
