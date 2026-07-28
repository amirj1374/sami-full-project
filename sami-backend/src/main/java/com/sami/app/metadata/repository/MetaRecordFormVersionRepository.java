package com.sami.app.metadata.repository;

import com.sami.app.metadata.domain.MetaRecordFormVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetaRecordFormVersionRepository extends JpaRepository<MetaRecordFormVersion, Long> {

    Optional<MetaRecordFormVersion> findByModuleCodeAndEntityCodeAndRecordId(
            String moduleCode, String entityCode, Long recordId);
}
