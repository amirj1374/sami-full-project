package com.sami.app.files.repository;

import com.sami.app.files.domain.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileStatusRepository extends JpaRepository<FileStatus, Long> {

    Optional<FileStatus> findByCode(String code);

    Optional<FileStatus> findFirstByIsDefaultTrue();

    /** Resolved by FLAG, never by name — see the config-as-data convention. */
    Optional<FileStatus> findFirstByIsAvailableStateTrue();

    Optional<FileStatus> findFirstByIsQuarantinedStateTrue();

    Optional<FileStatus> findFirstByIsDeletedStateTrue();

    Optional<FileStatus> findFirstByIsArchivedStateTrue();

    Optional<FileStatus> findFirstByIsProcessingStateTrue();

    List<FileStatus> findAllByOrderByDisplayOrderAsc();
}
