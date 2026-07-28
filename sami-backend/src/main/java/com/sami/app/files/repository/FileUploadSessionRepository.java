package com.sami.app.files.repository;

import com.sami.app.files.domain.FileUploadSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileUploadSessionRepository extends JpaRepository<FileUploadSession, Long> {

    @EntityGraph(attributePaths = {"category", "provider"})
    Optional<FileUploadSession> findBySessionUuid(UUID sessionUuid);

    @EntityGraph(attributePaths = {"category", "provider"})
    List<FileUploadSession> findAllByStatusAndExpiresAtBefore(String status, Instant cutoff);
}
