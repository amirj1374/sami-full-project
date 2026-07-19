package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.LicenseTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LicenseTransferRepository extends JpaRepository<LicenseTransfer, Long> {

    List<LicenseTransfer> findByLicenseIdOrderByTransferredAtDesc(Long licenseId);
}
