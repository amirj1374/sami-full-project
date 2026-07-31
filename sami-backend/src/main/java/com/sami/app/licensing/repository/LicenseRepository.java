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

    boolean existsByTenant_IdAndCode(Long tenantId, String code);

    @EntityGraph(attributePaths = {"status", "plan", "tenant", "licenseType", "paymentStatus", "expiryBehavior", "featureOverrides", "featureOverrides.feature"})
    List<License> findAllBy();

    @EntityGraph(attributePaths = {"status", "plan", "tenant", "licenseType", "paymentStatus", "expiryBehavior", "featureOverrides", "featureOverrides.feature"})
    List<License> findByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    boolean existsByLicenseKey(String licenseKey);

    @EntityGraph(attributePaths = {"status", "plan", "tenant", "licenseType", "expiryBehavior", "paymentStatus"})
    Optional<License> findByLicenseKey(String licenseKey);

    @EntityGraph(attributePaths = {"status", "plan", "tenant", "licenseType", "expiryBehavior", "paymentStatus", "featureOverrides", "featureOverrides.feature"})
    Optional<License> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"status", "plan", "tenant", "licenseType", "expiryBehavior", "paymentStatus", "featureOverrides", "featureOverrides.feature"})
    Optional<License> findWithDetailsByIdAndTenant_Id(Long id, Long tenantId);

    /**
     * Tenant-wide licences only. Company-scoped licences are deliberately not
     * selected without a trusted company context.
     */
    @EntityGraph(attributePaths = {"status", "plan", "licenseType", "expiryBehavior", "paymentStatus"})
    @Query("SELECT l FROM License l WHERE l.tenant.id = :tenantId AND l.companyId IS NULL "
            + "ORDER BY l.expirationDate DESC NULLS FIRST")
    List<License> findForTenant(Long tenantId);

    /** Licences past their term that are not yet marked expired (expiry sweep). */
    @EntityGraph(attributePaths = {"status", "plan", "tenant", "paymentStatus"})
    @Query("SELECT l FROM License l JOIN l.status s "
            + "WHERE l.expirationDate IS NOT NULL AND l.expirationDate < :cutoff AND s.isExpiredState = false")
    List<License> findLapsed(Instant cutoff);
}
