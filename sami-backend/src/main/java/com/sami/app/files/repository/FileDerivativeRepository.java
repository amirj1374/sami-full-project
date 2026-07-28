package com.sami.app.files.repository;

import com.sami.app.files.domain.FileDerivative;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileDerivativeRepository extends JpaRepository<FileDerivative, Long> {

    @EntityGraph(attributePaths = {"provider"})
    Optional<FileDerivative> findByVersionIdAndKind(Long versionId, String kind);

    @EntityGraph(attributePaths = {"provider"})
    List<FileDerivative> findAllByFileId(Long fileId);

    List<FileDerivative> findAllByVersionId(Long versionId);
}
