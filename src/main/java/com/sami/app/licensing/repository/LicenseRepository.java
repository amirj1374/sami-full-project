package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.License;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long>, JpaSpecificationExecutor<License> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"status", "plan", "tenant", "licenseType", "paymentStatus", "expiryBehavior"})
    List<License> findAllBy();

    boolean existsByLicenseKey(String licenseKey);

    @EntityGraph(attributePaths = {"status", "plan", "tenant", "licenseType", "expiryBehavior", "paymentStatus"})
    Optional<License> findByLicenseKey(String licenseKey);

    @EntityGraph(attributePaths = {"status", "plan", "tenant", "licenseType", "expiryBehavior", "paymentStatus", "featureOverrides"})
    Optional<License> findWithDetailsById(Long id);

    /**
     * Licences covering a tenant, most specific first: a company-scoped licence
     * outranks the tenant-wide one. Callers pick the first match.
     */
    @EntityGraph(attributePaths = {"status", "plan", "licenseType", "expiryBehavior", "paymentStatus"})
    @Query("SELECT l FROM License l WHERE l.tenant.id = :tenantId "
            + "ORDER BY CASE WHEN l.companyId IS NULL THEN 1 ELSE 0 END, l.expirationDate DESC NULLS FIRST")
    List<License> findForTenant(Long tenantId);

    /** Licences past their term that are not yet marked expired (expiry sweep). */
    @EntityGraph(attributePaths = {"status", "plan", "tenant", "paymentStatus"})
    @Query("SELECT l FROM License l JOIN l.status s "
            + "WHERE l.expirationDate IS NOT NULL AND l.expirationDate < :cutoff AND s.isExpiredState = false")
    List<License> findLapsed(Instant cutoff);
}
