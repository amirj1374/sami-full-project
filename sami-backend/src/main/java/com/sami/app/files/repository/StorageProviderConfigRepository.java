package com.sami.app.files.repository;

import com.sami.app.files.domain.StorageProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageProviderConfigRepository extends JpaRepository<StorageProviderConfig, Long> {

    Optional<StorageProviderConfig> findByCode(String code);

    Optional<StorageProviderConfig> findFirstByIsDefaultTrueAndEnabledTrue();

    List<StorageProviderConfig> findAllByEnabledTrueOrderByPriorityAsc();

    List<StorageProviderConfig> findAllByOrderByPriorityAsc();
}
