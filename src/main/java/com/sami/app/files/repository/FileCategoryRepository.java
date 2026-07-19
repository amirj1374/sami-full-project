package com.sami.app.files.repository;

import com.sami.app.files.domain.FileCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileCategoryRepository extends JpaRepository<FileCategory, Long> {

    /**
     * Eagerly fetches retentionPolicy and defaultProvider: both are dereferenced
     * when building responses, and a lazy proxy there caused a
     * LazyInitializationException on list endpoints elsewhere in this codebase.
     */
    @EntityGraph(attributePaths = {"retentionPolicy", "defaultProvider"})
    Optional<FileCategory> findByCode(String code);

    @EntityGraph(attributePaths = {"retentionPolicy", "defaultProvider"})
    List<FileCategory> findAllByOrderByDisplayOrderAsc();

    @Override
    @EntityGraph(attributePaths = {"retentionPolicy", "defaultProvider"})
    Optional<FileCategory> findById(Long id);
}
