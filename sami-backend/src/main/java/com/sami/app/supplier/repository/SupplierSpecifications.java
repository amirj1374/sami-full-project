package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupChannel;
import com.sami.app.supplier.domain.SupContact;
import com.sami.app.supplier.domain.SupTag;
import com.sami.app.supplier.domain.SupCategory;
import com.sami.app.supplier.domain.Supplier;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

/**
 * Composable, null-safe query fragments for supplier search. Multi-value
 * predicates use EXISTS subqueries so pagination never fans out.
 */
public final class SupplierSpecifications {

    private SupplierSpecifications() {
    }

    /** Default visibility: statuses flagged hiddenByDefault are excluded. */
    public static Specification<Supplier> visibleByDefault() {
        return (root, query, cb) -> cb.isFalse(root.get("status").get("hiddenByDefault"));
    }

    /**
     * Global search: code, company/display/legal name, owner, city, tax number,
     * any phone/email channel value, or any contact person name.
     */
    public static Specification<Supplier> globalSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> channel = query.subquery(Long.class);
            Root<SupChannel> ch = channel.from(SupChannel.class);
            channel.select(cb.literal(1L)).where(
                    cb.equal(ch.get("supplier"), root),
                    cb.like(cb.lower(ch.get("value")), pattern));

            Subquery<Long> contact = query.subquery(Long.class);
            Root<SupContact> co = contact.from(SupContact.class);
            contact.select(cb.literal(1L)).where(
                    cb.equal(co.get("supplier"), root),
                    cb.like(cb.lower(co.get("fullName")), pattern));

            return cb.or(
                    cb.like(cb.lower(root.get("supplierCode")), pattern),
                    cb.like(cb.lower(root.get("companyName")), pattern),
                    cb.like(cb.lower(root.get("displayName")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("legalName"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("ownerName"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("city"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("taxNumber"), "")), pattern),
                    cb.exists(channel),
                    cb.exists(contact));
        };
    }

    /** Suppliers with a phone/email channel containing the value. */
    public static Specification<Supplier> hasChannel(SupChannel.Kind kind, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pattern = "%" + value.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<SupChannel> ch = sub.from(SupChannel.class);
            sub.select(cb.literal(1L)).where(
                    cb.equal(ch.get("supplier"), root),
                    cb.equal(ch.get("kind"), kind),
                    cb.like(cb.lower(ch.get("value")), pattern));
            return cb.exists(sub);
        };
    }

    public static Specification<Supplier> contactNameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String pattern = "%" + name.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<SupContact> co = sub.from(SupContact.class);
            sub.select(cb.literal(1L)).where(
                    cb.equal(co.get("supplier"), root),
                    cb.like(cb.lower(co.get("fullName")), pattern));
            return cb.exists(sub);
        };
    }

    public static Specification<Supplier> cityContains(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        String pattern = "%" + city.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(cb.coalesce(root.get("city"), "")), pattern);
    }

    public static Specification<Supplier> hasAnyTag(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Supplier, SupTag> tags = root.join("tags", JoinType.INNER);
            return tags.get("id").in(tagIds);
        };
    }

    public static Specification<Supplier> hasAnyCategory(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Supplier, SupCategory> categories = root.join("categories", JoinType.INNER);
            return categories.get("id").in(categoryIds);
        };
    }

    public static Specification<Supplier> hasStatus(Long statusId) {
        return statusId == null ? null
                : (root, query, cb) -> cb.equal(root.get("status").get("id"), statusId);
    }

    public static Specification<Supplier> hasType(Long typeId) {
        return typeId == null ? null
                : (root, query, cb) -> cb.equal(root.get("type").get("id"), typeId);
    }

    public static Specification<Supplier> ratingAtLeast(BigDecimal minRating) {
        return minRating == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ratingAvg"), minRating);
    }
}
