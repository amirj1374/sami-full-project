package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PortalSessionRepository extends JpaRepository<PortalSession, Long> {

    Optional<PortalSession> findByTokenHash(String tokenHash);

    List<PortalSession> findAllByAccountIdAndRevokedAtIsNullOrderByIssuedAtDesc(Long accountId);

    /** Kills every live session for an account — used when locking or suspending. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PortalSession s SET s.revokedAt = :now, s.revokedReason = :reason "
            + "WHERE s.accountId = :accountId AND s.revokedAt IS NULL")
    int revokeAllForAccount(@Param("accountId") Long accountId,
                            @Param("now") Instant now,
                            @Param("reason") String reason);
}
