package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalAccountCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PortalAccountCapabilityRepository
        extends JpaRepository<PortalAccountCapability, PortalAccountCapability.Key> {

    List<PortalAccountCapability> findAllByIdAccountId(Long accountId);

    void deleteAllByIdAccountId(Long accountId);

    /** Capability CODES for an account — what the principal is built from. */
    @Query("SELECT c.code FROM PortalCapability c, PortalAccountCapability l "
            + "WHERE l.id.capabilityId = c.id AND l.id.accountId = :accountId")
    List<String> findCapabilityCodes(@Param("accountId") Long accountId);
}
