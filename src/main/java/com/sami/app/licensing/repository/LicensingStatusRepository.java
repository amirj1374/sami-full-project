package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.LicensingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LicensingStatusRepository extends JpaRepository<LicensingStatus, Long> {

    List<LicensingStatus> findByScopeOrderByDisplayOrderAsc(String scope);

    Optional<LicensingStatus> findByScopeAndCode(String scope, String code);

    Optional<LicensingStatus> findByScopeAndIsDefaultTrue(String scope);
}
