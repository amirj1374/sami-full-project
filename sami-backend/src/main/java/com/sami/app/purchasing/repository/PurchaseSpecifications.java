package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseItem;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Composable, null-safe query fragments for purchase search. */
public final class PurchaseSpecifications {

    private PurchaseSpecifications() {
    }

    public static Specification<Purchase> hasTenant(Long tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    /** Global search: purchase number, supplier name/code, or notes. */
    public static Specification<Purchase> globalSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("purchaseNumber")), pattern),
                cb.like(cb.lower(root.get("supplier").get("displayName")), pattern),
                cb.like(cb.lower(root.get("supplier").get("supplierCode")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), pattern));
    }

    /** Purchases containing a line for the given product. */
    public static Specification<Purchase> containsProduct(Long productId) {
        if (productId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PurchaseItem> item = sub.from(PurchaseItem.class);
            sub.select(cb.literal(1L)).where(
                    cb.equal(item.get("purchase"), root),
                    cb.equal(item.get("product").get("id"), productId));
            return cb.exists(sub);
        };
    }

    public static Specification<Purchase> hasSupplier(Long supplierId) {
        return supplierId == null ? null
                : (root, query, cb) -> cb.equal(root.get("supplier").get("id"), supplierId);
    }

    public static Specification<Purchase> hasStatus(Long statusId) {
        return statusId == null ? null
                : (root, query, cb) -> cb.equal(root.get("status").get("id"), statusId);
    }

    public static Specification<Purchase> hasType(Long typeId) {
        return typeId == null ? null
                : (root, query, cb) -> cb.equal(root.get("type").get("id"), typeId);
    }

    public static Specification<Purchase> hasWarehouse(Long warehouseId) {
        return warehouseId == null ? null
                : (root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId);
    }

    public static Specification<Purchase> createdBy(Long userId) {
        return userId == null ? null
                : (root, query, cb) -> cb.equal(root.get("createdBy"), userId);
    }

    public static Specification<Purchase> createdOnOrAfter(LocalDate from) {
        if (from == null) {
            return null;
        }
        Instant threshold = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), threshold);
    }

    public static Specification<Purchase> createdBefore(LocalDate to) {
        if (to == null) {
            return null;
        }
        Instant threshold = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), threshold);
    }
}
