package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalDocumentGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortalDocumentGrantRepository extends JpaRepository<PortalDocumentGrant, Long> {

    List<PortalDocumentGrant> findAllByAccountIdAndRevokedAtIsNullOrderByGrantedAtDesc(Long accountId);

    /** The authorisation check for every download: grant must exist and be live. */
    Optional<PortalDocumentGrant> findByAccountIdAndFileUuidAndRevokedAtIsNull(
            Long accountId, UUID fileUuid);

    long countByAccountIdAndRevokedAtIsNull(Long accountId);
}
