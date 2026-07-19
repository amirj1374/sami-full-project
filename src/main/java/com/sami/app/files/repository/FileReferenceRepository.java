package com.sami.app.files.repository;

import com.sami.app.files.domain.FileReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileReferenceRepository extends JpaRepository<FileReference, Long> {

    List<FileReference> findAllByFileId(Long fileId);

    List<FileReference> findAllByModuleCodeAndEntityCodeAndRecordId(
            String moduleCode, String entityCode, Long recordId);

    long countByFileId(Long fileId);

    void deleteAllByFileId(Long fileId);
}
