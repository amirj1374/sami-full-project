package com.sami.app.files.repository;

import com.sami.app.files.domain.FileVersion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    @EntityGraph(attributePaths = {"provider"})
    List<FileVersion> findAllByFileIdOrderByVersionMajorDescVersionMinorDescRevisionDesc(Long fileId);

    @EntityGraph(attributePaths = {"provider"})
    Optional<FileVersion> findFirstByFileIdAndIsCurrentTrue(Long fileId);

    @EntityGraph(attributePaths = {"provider"})
    Optional<FileVersion> findByFileIdAndLabel(Long fileId, String label);

    @Override
    @EntityGraph(attributePaths = {"provider"})
    Optional<FileVersion> findById(Long id);

    long countByFileId(Long fileId);
}
