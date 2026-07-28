package com.sami.app.files.repository;

import com.sami.app.files.domain.FileFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileFolderRepository extends JpaRepository<FileFolder, Long> {

    Optional<FileFolder> findByPath(String path);

    List<FileFolder> findAllByParentIdOrderByNameAsc(Long parentId);

    List<FileFolder> findAllByParentIsNullOrderByNameAsc();

    /** Subtree lookup by materialised-path prefix. */
    List<FileFolder> findAllByPathStartingWith(String prefix);

    boolean existsByParentId(Long parentId);
}
