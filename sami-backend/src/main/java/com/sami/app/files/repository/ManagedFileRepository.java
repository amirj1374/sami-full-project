package com.sami.app.files.repository;

import com.sami.app.files.domain.ManagedFile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface ManagedFileRepository
        extends JpaRepository<ManagedFile, Long>, JpaSpecificationExecutor<ManagedFile> {

    /**
     * Every read path eagerly fetches category, status, folder AND
     * retentionPolicy. All four are dereferenced by FileResponse.from.
     *
     * <p>retentionPolicy was missing here originally and produced exactly the
     * LazyInitializationException this comment was written to prevent — proof
     * that the entity graph must be kept in step with the response mapper, not
     * merely documented.
     */
    @EntityGraph(attributePaths = {"category", "status", "folder", "retentionPolicy"})
    Optional<ManagedFile> findByFileUuid(UUID fileUuid);

    @Override
    @EntityGraph(attributePaths = {"category", "status", "folder", "retentionPolicy"})
    Optional<ManagedFile> findById(Long id);

    @EntityGraph(attributePaths = {"category", "status", "folder", "retentionPolicy"})
    Optional<ManagedFile> findByFileCode(String fileCode);

    /** Dedupe lookup: same content, same category, still live. */
    @EntityGraph(attributePaths = {"category", "status", "folder", "retentionPolicy"})
    Optional<ManagedFile> findFirstByChecksumSha256AndCategoryIdAndDeletedAtIsNull(
            String checksumSha256, Long categoryId);

    @EntityGraph(attributePaths = {"category", "status", "folder", "retentionPolicy"})
    List<ManagedFile> findAllByModuleCodeAndEntityCodeAndEntityIdAndDeletedAtIsNull(
            String moduleCode, String entityCode, Long entityId);

    @Query("SELECT f FROM ManagedFile f WHERE f.retentionExpiresAt IS NOT NULL "
            + "AND f.retentionExpiresAt <= :now AND f.deletedAt IS NULL "
            + "AND (f.legalHoldUntil IS NULL OR f.legalHoldUntil <= :now)")
    List<ManagedFile> findExpired(@Param("now") Instant now);

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM ManagedFile f WHERE f.deletedAt IS NULL")
    long totalBytes();

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM ManagedFile f "
            + "WHERE f.deletedAt IS NULL AND f.companyId = :companyId")
    long totalBytesByCompany(@Param("companyId") Long companyId);

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM ManagedFile f "
            + "WHERE f.deletedAt IS NULL AND f.branchId = :branchId")
    long totalBytesByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM ManagedFile f "
            + "WHERE f.deletedAt IS NULL AND f.moduleCode = :moduleCode")
    long totalBytesByModule(@Param("moduleCode") String moduleCode);

    long countByDeletedAtIsNull();

    @Query(value = "SELECT nextval('file_code_seq')", nativeQuery = true)
    Long nextCodeSequence();

    /**
     * Specification queries do NOT inherit an {@code @EntityGraph} declared on
     * other methods, so this override is required: without it every association
     * loads lazily and the response mapper throws
     * {@code LazyInitializationException} once the session closes. This is the
     * list endpoint — the one a get-by-id test never exercises.
     */
    @Override
    @EntityGraph(attributePaths = {"category", "status", "folder", "retentionPolicy"})
    Page<ManagedFile> findAll(Specification<ManagedFile> spec, Pageable pageable);
}
