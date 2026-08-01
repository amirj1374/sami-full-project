package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data-access for {@link CustomerNote}. */
public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

    /** Notes with the given visibility plus the caller's own, newest first. */
    @Query("""
            SELECT n FROM CustomerNote n
            WHERE n.tenantId = :tenantId AND n.customer.id = :customerId
              AND (n.visibility = :publicVisibility OR n.authorId = :viewerId)
            ORDER BY n.createdAt DESC
            """)
    Page<CustomerNote> findVisible(@Param("tenantId") Long tenantId,
                                   @Param("customerId") Long customerId,
                                   @Param("viewerId") Long viewerId,
                                   @Param("publicVisibility") CustomerNote.Visibility publicVisibility,
                                   Pageable pageable);

    /** Re-parents notes during a merge (bulk SQL, no entity loading). */
    @Modifying
    @Query(value = "UPDATE customer_notes SET customer_id = :targetId WHERE tenant_id = :tenantId AND customer_id = :sourceId",
            nativeQuery = true)
    int moveAll(@Param("tenantId") Long tenantId, @Param("sourceId") Long sourceId,
                @Param("targetId") Long targetId);

    java.util.Optional<CustomerNote> findByIdAndTenantId(Long id, Long tenantId);
}
