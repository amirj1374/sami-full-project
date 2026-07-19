package com.sami.app.files.dto;

import com.sami.app.files.domain.FileCategory;
import com.sami.app.files.domain.FileDerivative;
import com.sami.app.files.domain.FileFolder;
import com.sami.app.files.domain.FileProcessorConfig;
import com.sami.app.files.domain.FileReference;
import com.sami.app.files.domain.FileScanResult;
import com.sami.app.files.domain.FileStatus;
import com.sami.app.files.domain.FileTag;
import com.sami.app.files.domain.FileVersion;
import com.sami.app.files.domain.ManagedFile;
import com.sami.app.files.domain.RetentionPolicy;
import com.sami.app.files.domain.StorageProviderConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request/response records for the files module.
 *
 * <p>Responses expose {@code fileUuid} and never a storage key, provider or
 * path — the API surface enforces the same rule the architecture requires of
 * business modules.
 */
public final class FileDtos {

    private FileDtos() {
    }

    // ---- Files --------------------------------------------------------------

    public record FileResponse(UUID fileUuid,
                               String fileCode,
                               String name,
                               String originalFilename,
                               String description,
                               String categoryCode,
                               String categoryName,
                               String statusCode,
                               String statusName,
                               boolean downloadable,
                               Long folderId,
                               String folderPath,
                               String moduleCode,
                               String entityCode,
                               Long entityId,
                               Long companyId,
                               Long branchId,
                               Long ownerId,
                               String ownerEmail,
                               long sizeBytes,
                               String extension,
                               String mimeType,
                               String checksumSha256,
                               Map<String, Object> metadata,
                               String currentVersion,
                               String retentionPolicyCode,
                               Instant retentionExpiresAt,
                               Instant legalHoldUntil,
                               boolean underLegalHold,
                               boolean deleted,
                               Instant createdAt,
                               Instant updatedAt) {

        public static FileResponse from(ManagedFile file, String currentVersionLabel) {
            return new FileResponse(
                    file.getFileUuid(),
                    file.getFileCode(),
                    file.getName(),
                    file.getOriginalFilename(),
                    file.getDescription(),
                    file.getCategory().getCode(),
                    file.getCategory().getName(),
                    file.getStatus().getCode(),
                    file.getStatus().getName(),
                    file.getStatus().isAllowsDownload(),
                    file.getFolder() == null ? null : file.getFolder().getId(),
                    file.getFolder() == null ? null : file.getFolder().getPath(),
                    file.getModuleCode(),
                    file.getEntityCode(),
                    file.getEntityId(),
                    file.getCompanyId(),
                    file.getBranchId(),
                    file.getOwnerId(),
                    file.getOwnerEmail(),
                    file.getSizeBytes(),
                    file.getExtension(),
                    file.getMimeType(),
                    file.getChecksumSha256(),
                    file.getMetadata(),
                    currentVersionLabel,
                    file.getRetentionPolicy() == null ? null : file.getRetentionPolicy().getCode(),
                    file.getRetentionExpiresAt(),
                    file.getLegalHoldUntil(),
                    file.isUnderLegalHold(),
                    file.isDeleted(),
                    file.getCreatedAt(),
                    file.getUpdatedAt());
        }
    }

    public record UpdateFileRequest(@Size(max = 255) String name,
                                    @Size(max = 1000) String description,
                                    Long folderId,
                                    Instant legalHoldUntil,
                                    String retentionPolicyCode) {
    }

    public record VersionResponse(Long id,
                                  String label,
                                  int major,
                                  int minor,
                                  int revision,
                                  long sizeBytes,
                                  String checksumSha256,
                                  String mimeType,
                                  String comment,
                                  boolean current,
                                  String providerCode,
                                  String createdByEmail,
                                  Instant createdAt) {

        public static VersionResponse from(FileVersion v) {
            return new VersionResponse(v.getId(), v.getLabel(), v.getVersionMajor(),
                    v.getVersionMinor(), v.getRevision(), v.getSizeBytes(),
                    v.getChecksumSha256(), v.getMimeType(), v.getComment(), v.isCurrent(),
                    v.getProvider() == null ? null : v.getProvider().getCode(),
                    v.getCreatedByEmail(), v.getCreatedAt());
        }
    }

    public record DerivativeResponse(Long id,
                                     String kind,
                                     String mimeType,
                                     Long sizeBytes,
                                     Integer width,
                                     Integer height,
                                     Integer pageCount,
                                     Long durationMs,
                                     boolean hasText,
                                     String generatorKey,
                                     String status,
                                     Instant generatedAt) {

        public static DerivativeResponse from(FileDerivative d) {
            return new DerivativeResponse(d.getId(), d.getKind(), d.getMimeType(),
                    d.getSizeBytes(), d.getWidth(), d.getHeight(), d.getPageCount(),
                    d.getDurationMs(), d.getTextContent() != null, d.getGeneratorKey(),
                    d.getStatus(), d.getGeneratedAt());
        }
    }

    public record ScanResultResponse(String scannerKey, String verdict, String threat,
                                     Instant scannedAt) {

        public static ScanResultResponse from(FileScanResult r) {
            return new ScanResultResponse(r.getScannerKey(), r.getVerdict(),
                    r.getThreat(), r.getScannedAt());
        }
    }

    public record ReferenceResponse(Long id, String moduleCode, String entityCode,
                                    Long recordId, String role) {

        public static ReferenceResponse from(FileReference r) {
            return new ReferenceResponse(r.getId(), r.getModuleCode(), r.getEntityCode(),
                    r.getRecordId(), r.getRole());
        }
    }

    public record AddReferenceRequest(@NotBlank String moduleCode,
                                      @NotBlank String entityCode,
                                      @NotNull Long recordId,
                                      String role) {
    }

    public record AssignTagsRequest(@NotNull List<Long> tagIds) {
    }

    // ---- Folders ------------------------------------------------------------

    public record FolderResponse(Long id, Long parentId, String name, String path,
                                 String description, boolean virtual, boolean smart,
                                 Map<String, Object> smartQuery, boolean system) {

        public static FolderResponse from(FileFolder f) {
            return new FolderResponse(f.getId(),
                    f.getParent() == null ? null : f.getParent().getId(),
                    f.getName(), f.getPath(), f.getDescription(),
                    f.isVirtual(), f.isSmart(), f.getSmartQuery(), f.isSystem());
        }
    }

    public record FolderRequest(Long parentId,
                                @NotBlank @Size(max = 160) String name,
                                @Size(max = 500) String description,
                                boolean virtual,
                                boolean smart,
                                Map<String, Object> smartQuery) {
    }

    public record RenameFolderRequest(@NotBlank @Size(max = 160) String name) {
    }

    // ---- Catalog ------------------------------------------------------------

    public record CategoryResponse(Long id, String code, String name, String description,
                                   Long maxBytes, List<String> allowedMimeTypes,
                                   List<String> allowedExtensions, boolean scanRequired,
                                   boolean versioningEnabled, boolean dedupeEnabled,
                                   List<String> processors, String retentionPolicyCode,
                                   String defaultProviderCode) {

        public static CategoryResponse from(FileCategory c) {
            return new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDescription(),
                    c.getMaxBytes(), c.getAllowedMimeTypes(), c.getAllowedExtensions(),
                    c.isScanRequired(), c.isVersioningEnabled(), c.isDedupeEnabled(),
                    c.getProcessors(),
                    c.getRetentionPolicy() == null ? null : c.getRetentionPolicy().getCode(),
                    c.getDefaultProvider() == null ? null : c.getDefaultProvider().getCode());
        }
    }

    public record StatusResponse(Long id, String code, String name, boolean isDefault,
                                 boolean available, boolean quarantined, boolean archived,
                                 boolean deleted, boolean allowsDownload, boolean allowsNewVersion) {

        public static StatusResponse from(FileStatus s) {
            return new StatusResponse(s.getId(), s.getCode(), s.getName(), s.isDefault(),
                    s.isAvailableState(), s.isQuarantinedState(), s.isArchivedState(),
                    s.isDeletedState(), s.isAllowsDownload(), s.isAllowsNewVersion());
        }
    }

    public record ProviderResponse(Long id, String code, String name, String handlerKey,
                                   boolean isDefault, boolean enabled, boolean supportsStreaming,
                                   boolean supportsSignedUrl, boolean archiveTier,
                                   boolean handlerRegistered, boolean available) {

        public static ProviderResponse from(StorageProviderConfig p,
                                            boolean handlerRegistered, boolean available) {
            return new ProviderResponse(p.getId(), p.getCode(), p.getName(), p.getHandlerKey(),
                    p.isDefault(), p.isEnabled(), p.isSupportsStreaming(),
                    p.isSupportsSignedUrl(), p.isArchiveTier(), handlerRegistered, available);
        }
    }

    public record ProcessorResponse(Long id, String code, String name, String handlerKey,
                                    Map<String, Object> config, boolean enabled, boolean async,
                                    boolean failsUpload, int runOrder, boolean handlerRegistered) {

        public static ProcessorResponse from(FileProcessorConfig p, boolean handlerRegistered) {
            return new ProcessorResponse(p.getId(), p.getCode(), p.getName(), p.getHandlerKey(),
                    p.getConfig(), p.isEnabled(), p.isAsync(), p.isFailsUpload(),
                    p.getRunOrder(), handlerRegistered);
        }
    }

    public record TagResponse(Long id, String code, String name, String color,
                              boolean confidential) {

        public static TagResponse from(FileTag t) {
            return new TagResponse(t.getId(), t.getCode(), t.getName(), t.getColor(),
                    t.isConfidential());
        }
    }

    public record RetentionPolicyResponse(Long id, String code, String name, Integer retainDays,
                                          String actionOnExpiry, boolean allowsLegalHold,
                                          boolean isDefault, boolean permanent) {

        public static RetentionPolicyResponse from(RetentionPolicy p) {
            return new RetentionPolicyResponse(p.getId(), p.getCode(), p.getName(),
                    p.getRetainDays(), p.getActionOnExpiry(), p.isAllowsLegalHold(),
                    p.isDefault(), p.isPermanent());
        }
    }

    /**
     * The whole configuration surface plus which plugin beans are actually
     * registered — so an operator can see that e.g. the OCR processor is enabled
     * in config but has no implementation.
     */
    public record CatalogResponse(List<CategoryResponse> categories,
                                  List<StatusResponse> statuses,
                                  List<ProviderResponse> providers,
                                  List<ProcessorResponse> processors,
                                  List<TagResponse> tags,
                                  List<RetentionPolicyResponse> retentionPolicies,
                                  List<String> registeredStorageHandlers,
                                  List<String> registeredProcessors,
                                  List<String> registeredRetentionActions,
                                  List<String> reports) {
    }
}
