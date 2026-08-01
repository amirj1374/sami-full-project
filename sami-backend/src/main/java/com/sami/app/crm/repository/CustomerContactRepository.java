package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data-access for {@link CustomerContact}; backs the duplicate checks. */
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> {

    /**
     * Customers (not merged away) owning a contact of the given kind/value —
     * the primitive behind phone/email duplicate detection. Pass {@code -1} as
     * {@code excludeId} when no customer should be excluded.
     */
    @Query("""
            SELECT DISTINCT c.customer.id FROM CustomerContact c
            WHERE c.kind = :kind AND lower(c.value) = lower(:value)
              AND c.customer.tenantId = :tenantId
              AND c.customer.mergedInto IS NULL
              AND c.customer.id <> :excludeId
            """)
    List<Long> findOwnerIds(@Param("kind") CustomerContact.Kind kind,
                            @Param("value") String value,
                            @Param("excludeId") long excludeId,
                            @Param("tenantId") Long tenantId);
}
