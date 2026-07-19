package com.sami.app.files.repository;

import com.sami.app.files.domain.FileProcessorConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileProcessorConfigRepository extends JpaRepository<FileProcessorConfig, Long> {

    Optional<FileProcessorConfig> findByCode(String code);

    List<FileProcessorConfig> findAllByCodeInAndEnabledTrueOrderByRunOrderAsc(List<String> codes);

    List<FileProcessorConfig> findAllByOrderByRunOrderAsc();
}
