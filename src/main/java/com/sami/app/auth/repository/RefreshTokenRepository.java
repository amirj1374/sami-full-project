package com.sami.app.auth.repository;

import com.sami.app.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/** Data-access for {@link RefreshToken}. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes every active token of a user in one statement (logout-everywhere on
     * password change or account deactivation).
     *
     * @return the number of tokens revoked
     */
    @Modifying
    @Query("""
            update RefreshToken t
               set t.revokedAt = :now
             where t.user.id = :userId
               and t.revokedAt is null
            """)
    int revokeAllActiveForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
