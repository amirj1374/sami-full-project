package com.sami.app.files.repository;

import com.sami.app.files.domain.FileScanResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileScanResultRepository extends JpaRepository<FileScanResult, Long> {

    List<FileScanResult> findAllByFileIdOrderByScannedAtDesc(Long fileId);

    Optional<FileScanResult> findFirstByVersionIdOrderByScannedAtDesc(Long versionId);
}
