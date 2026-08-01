package com.sami.app.crm.repository;

import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.CustomerAddress;
import com.sami.app.crm.domain.CustomerContact;
import com.sami.app.crm.domain.CustomerEvent;
import com.sami.app.crm.domain.CustomerTag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Composable, null-safe query fragments for customer search. Multi-value
 * predicates (contacts, addresses, tags, events) use EXISTS subqueries so the
 * paginated root query never fans out into duplicate rows.
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> hasTenant(Long tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    /** Merged-away records never appear in listings or checks. */
    public static Specification<Customer> notMerged() {
        return (root, query, cb) -> cb.isNull(root.get("mergedInto"));
    }

    /** Default visibility: statuses flagged hiddenByDefault are excluded. */
    public static Specification<Customer> visibleByDefault() {
        return (root, query, cb) -> cb.isFalse(root.get("status").get("hiddenByDefault"));
    }

    /**
     * Global search: code, names, company, national code, passport number, or
     * any phone/email value.
     */
    public static Specification<Customer> globalSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> contact = query.subquery(Long.class);
            Root<CustomerContact> c = contact.from(CustomerContact.class);
            contact.select(cb.literal(1L)).where(
                    cb.equal(c.get("customer"), root),
                    cb.like(cb.lower(c.get("value")), pattern));

            return cb.or(
                    cb.like(cb.lower(root.get("customerCode")), pattern),
                    cb.like(cb.lower(root.get("displayName")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("firstName"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("lastName"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("companyName"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("nationalCode"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("passportNumber"), "")), pattern),
                    cb.exists(contact));
        };
    }

    /** Customers owning a contact of the given kind containing the value. */
    public static Specification<Customer> hasContact(CustomerContact.Kind kind, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pattern = "%" + value.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<CustomerContact> c = sub.from(CustomerContact.class);
            sub.select(cb.literal(1L)).where(
                    cb.equal(c.get("customer"), root),
                    cb.equal(c.get("kind"), kind),
                    cb.like(cb.lower(c.get("value")), pattern));
            return cb.exists(sub);
        };
    }

    public static Specification<Customer> cityContains(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        String pattern = "%" + city.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<CustomerAddress> a = sub.from(CustomerAddress.class);
            sub.select(cb.literal(1L)).where(
                    cb.equal(a.get("customer"), root),
                    cb.like(cb.lower(cb.coalesce(a.get("city"), "")), pattern));
            return cb.exists(sub);
        };
    }

    /** Customers carrying ANY of the given tags. */
    public static Specification<Customer> hasAnyTag(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Customer, CustomerTag> tags = root.join("tags", JoinType.INNER);
            return tags.get("id").in(tagIds);
        };
    }

    /**
     * Customers with at least one timeline event of the given type — this is how
     * "purchase history" / "repair history" searches work once those modules
     * record their events. With {@code sinceDays}, only recent events count
     * ("purchased in the last 30 days").
     */
    public static Specification<Customer> hasEventOfType(String eventType, Integer sinceDays) {
        if (eventType == null || eventType.isBlank()) {
            return null;
        }
        Instant threshold = sinceDays == null ? null
                : Instant.now().minus(sinceDays, ChronoUnit.DAYS);
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<CustomerEvent> e = sub.from(CustomerEvent.class);
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(e.get("customerId"), root.get("id")));
            predicates.add(cb.equal(e.get("tenantId"), root.get("tenantId")));
            predicates.add(cb.equal(e.get("eventType"), eventType));
            if (threshold != null) {
                predicates.add(cb.greaterThanOrEqualTo(e.get("occurredAt"), threshold));
            }
            sub.select(cb.literal(1L)).where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            return cb.exists(sub);
        };
    }

    /** Customers with NO timeline activity in the last N days ("inactive"). */
    public static Specification<Customer> noEventSince(Integer days) {
        if (days == null) {
            return null;
        }
        Instant threshold = Instant.now().minus(days, ChronoUnit.DAYS);
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<CustomerEvent> e = sub.from(CustomerEvent.class);
            sub.select(cb.literal(1L)).where(
                    cb.equal(e.get("customerId"), root.get("id")),
                    cb.equal(e.get("tenantId"), root.get("tenantId")),
                    cb.greaterThanOrEqualTo(e.get("occurredAt"), threshold));
            return cb.not(cb.exists(sub));
        };
    }

    public static Specification<Customer> hasStatus(Long statusId) {
        return statusId == null ? null
                : (root, query, cb) -> cb.equal(root.get("status").get("id"), statusId);
    }

    public static Specification<Customer> hasType(Long typeId) {
        return typeId == null ? null
                : (root, query, cb) -> cb.equal(root.get("type").get("id"), typeId);
    }

    public static Specification<Customer> hasSource(Long sourceId) {
        return sourceId == null ? null
                : (root, query, cb) -> cb.equal(root.get("source").get("id"), sourceId);
    }

    public static Specification<Customer> nationalCodeEquals(String nationalCode) {
        return nationalCode == null || nationalCode.isBlank() ? null
                : (root, query, cb) -> cb.equal(root.get("nationalCode"), nationalCode);
    }
}
