package com.sami.app.files.repository;

import com.sami.app.files.domain.FileTagLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileTagLinkRepository extends JpaRepository<FileTagLink, FileTagLink.Key> {

    List<FileTagLink> findAllByIdFileId(Long fileId);

    void deleteAllByIdFileId(Long fileId);

    @Query("SELECT l.id.fileId FROM FileTagLink l WHERE l.id.tagId IN :tagIds")
    List<Long> findFileIdsByTagIds(@Param("tagIds") List<Long> tagIds);
}
