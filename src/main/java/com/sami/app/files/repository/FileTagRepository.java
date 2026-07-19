package com.sami.app.files.repository;

import com.sami.app.files.domain.FileTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileTagRepository extends JpaRepository<FileTag, Long> {

    Optional<FileTag> findByCode(String code);

    List<FileTag> findAllByOrderByDisplayOrderAsc();

    List<FileTag> findAllByIdIn(List<Long> ids);
}
