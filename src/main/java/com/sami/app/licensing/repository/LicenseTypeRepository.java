package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.LicenseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LicenseTypeRepository extends JpaRepository<LicenseType, Long> {

    List<LicenseType> findAllByOrderByDisplayOrderAsc();

    Optional<LicenseType> findByCode(String code);

    Optional<LicenseType> findByIsDefaultTrue();
}
