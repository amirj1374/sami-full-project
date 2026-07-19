package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortalOtpChallengeRepository extends JpaRepository<PortalOtpChallenge, Long> {
    Optional<PortalOtpChallenge> findFirstByAccountIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            Long accountId, String purpose);
}
