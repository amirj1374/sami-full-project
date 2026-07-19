package com.sami.app.files.repository;

import com.sami.app.files.domain.StorageQuota;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long> {

    @EntityGraph(attributePaths = {"provider"})
    List<StorageQuota> findAllByEnabledTrue();

    @EntityGraph(attributePaths = {"provider"})
    List<StorageQuota> findAllByScopeKindAndEnabledTrue(String scopeKind);
}
