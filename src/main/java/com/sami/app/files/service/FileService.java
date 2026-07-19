package com.sami.app.files.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.files.FilesProperties;
import com.sami.app.files.domain.FileCategory;
import com.sami.app.files.domain.FileDerivative;
import com.sami.app.files.domain.FileProcessorConfig;
import com.sami.app.files.domain.FileReference;
import com.sami.app.files.domain.FileScanResult;
import com.sami.app.files.domain.FileStatus;
import com.sami.app.files.domain.FileVersion;
import com.sami.app.files.domain.ManagedFile;
import com.sami.app.files.domain.RetentionPolicy;
import com.sami.app.files.domain.StorageProviderConfig;
import com.sami.app.files.event.FileDomainEvent;
import com.sami.app.files.repository.FileCategoryRepository;
import com.sami.app.files.repository.FileDerivativeRepository;
import com.sami.app.files.repository.FileFolderRepository;
import com.sami.app.files.repository.FileProcessorConfigRepository;
import com.sami.app.files.repository.FileReferenceRepository;
import com.sami.app.files.repository.FileScanResultRepository;
import com.sami.app.files.repository.FileStatusRepository;
import com.sami.app.files.repository.FileVersionRepository;
import com.sami.app.files.repository.ManagedFileRepository;
import com.sami.app.files.repository.StorageProviderConfigRepository;
import com.sami.app.files.spi.FileContent;
import com.sami.app.files.spi.FileContents;
import com.sami.app.files.spi.FileProcessor;
import com.sami.app.files.spi.FileProcessorRegistry;
import com.sami.app.files.spi.StorageProviderHandler;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The module's public service and the only way binary content enters the ERP.
 *
 * <p>Business modules call {@link #upload} and keep the returned
 * {@code fileUuid}. They never see a storage key, a provider or a path, so
 * storage can be reconfigured or migrated without touching a business table.
 *
 * <p>Uploads are staged to a temporary file first. That is what makes checksum
 * computation, validation, dedupe and multi-pass processing possible without
 * holding the payload in heap — the ERP stores scanned contracts and repair
 * photo sets, not just avatars.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final ManagedFileRepository fileRepository;
    private final FileVersionRepository versionRepository;
    private final FileDerivativeRepository derivativeRepository;
    private final FileScanResultRepository scanRepository;
    private final FileReferenceRepository referenceRepository;
    private final FileCategoryRepository categoryRepository;
    private final FileStatusRepository statusRepository;
    private final FileFolderRepository folderRepository;
    private final FileProcessorConfigRepository processorConfigRepository;
    private final StorageProviderConfigRepository providerRepository;
    private final FileProcessorRegistry processorRegistry;
    private final FileValidationService validationService;
    private final StorageQuotaService quotaService;
    private final FileAuditService auditService;
    private final FilesProperties properties;
    private final TenantDefaults tenantDefaults;
    private final ApplicationEventPublisher events;

    // ---- Upload -------------------------------------------------------------

    /**
     * Stores new content as a brand-new file.
     *
     * @param command upload metadata; {@code categoryCode} selects the validation
     *                and processing contract
     * @param source  the incoming bytes; consumed once and staged to disk
     */
    @Transactional
    public ManagedFile upload(UploadCommand command, InputStream source) {
        FileCategory category = categoryRepository.findByCode(command.categoryCode())
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unknown file category: " + command.categoryCode()));

        Staged staged = stage(source, command.filename(), command.contentType());
        try {
            validationService.validateUpload(category, command.filename(),
                    command.contentType(), staged.size());
            validationService.verifyChecksum(command.declaredChecksum(), staged.checksum());

            // Dedupe is per (tenant, category, checksum) and only considers live
            // files, so re-uploading a deleted file legitimately creates a new one.
            if (category.isDedupeEnabled()) {
                Optional<ManagedFile> existing = fileRepository
                        .findFirstByChecksumSha256AndCategoryIdAndDeletedAtIsNull(
                                staged.checksum(), category.getId());
                if (existing.isPresent()) {
                    ManagedFile duplicate = existing.get();
                    addReferenceIfRequested(duplicate, command);
                    return duplicate;
                }
            }

            StorageProviderConfig provider = resolveProvider(category);
            StorageProviderHandler handler = validationService.requireAvailable(provider);

            ManagedFile file = newFile(command, category, staged);
            quotaService.enforce(file, staged.size());

            String storageKey = store(handler, provider, category.getCode(), staged);
            file = fileRepository.save(file);

            FileVersion version = createVersion(file, provider, storageKey, staged, 1, 0, 0,
                    "Initial version");
            file.setCurrentVersionId(version.getId());

            runProcessors(file, version, category, staged);

            file.setStatus(resolveStatusAfterProcessing(file));
            fileRepository.save(file);

            addReferenceIfRequested(file, command);

            auditService.record("file", file.getId(), FileAuditService.ACTION_UPLOADED, null,
                    Map.of("name", file.getName(), "size", file.getSizeBytes(),
                            "category", category.getCode(), "checksum", staged.checksum()));
            publish(FileDomainEvent.FILE_UPLOADED, file,
                    Map.of("size", file.getSizeBytes(), "category", category.getCode()));
            return file;
        } finally {
            staged.discard();
        }
    }

    /** Adds a new version to an existing file. Rollback targets remain intact. */
    @Transactional
    public FileVersion addVersion(UUID fileUuid, InputStream source, String filename,
                                  String contentType, boolean major, String comment) {
        ManagedFile file = requireFile(fileUuid);
        FileCategory category = file.getCategory();

        if (!category.isVersioningEnabled()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Versioning is disabled for category " + category.getCode());
        }
        if (!file.getStatus().isAllowsNewVersion()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A file in status '%s' cannot receive a new version"
                            .formatted(file.getStatus().getCode()));
        }

        Staged staged = stage(source, filename, contentType);
        try {
            validationService.validateUpload(category, filename, contentType, staged.size());

            FileVersion current = versionRepository.findFirstByFileIdAndIsCurrentTrue(file.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                            "File %s has no current version".formatted(file.getFileCode())));

            // Version conflict: an identical payload is not a new version.
            if (current.getChecksumSha256().equalsIgnoreCase(staged.checksum())) {
                throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                        "The uploaded content is identical to version " + current.getLabel());
            }

            StorageProviderConfig provider = resolveProvider(category);
            StorageProviderHandler handler = validationService.requireAvailable(provider);
            quotaService.enforce(file, staged.size());

            String storageKey = store(handler, provider, category.getCode(), staged);

            int nextMajor = major ? current.getVersionMajor() + 1 : current.getVersionMajor();
            int nextMinor = major ? 0 : current.getVersionMinor() + 1;

            // Flush the clear before inserting the replacement: uq_file_versions_current
            // is a partial unique index on (file_id) WHERE is_current, and Hibernate is
            // free to order the INSERT before the UPDATE within one transaction.
            current.setCurrent(false);
            versionRepository.saveAndFlush(current);

            FileVersion version = createVersion(file, provider, storageKey, staged,
                    nextMajor, nextMinor, 0, comment);

            file.setCurrentVersionId(version.getId());
            file.setSizeBytes(staged.size());
            file.setChecksumSha256(staged.checksum());
            file.setMimeType(staged.contentType());
            runProcessors(file, version, category, staged);
            fileRepository.save(file);

            auditService.record("file", file.getId(), FileAuditService.ACTION_VERSION_CREATED,
                    Map.of("version", current.getLabel()), Map.of("version", version.getLabel()));
            publish(FileDomainEvent.VERSION_CREATED, file, Map.of("version", version.getLabel()));
            return version;
        } finally {
            staged.discard();
        }
    }

    /** Promotes an earlier version to current. Never destroys bytes. */
    @Transactional
    public FileVersion rollback(UUID fileUuid, String label) {
        ManagedFile file = requireFile(fileUuid);
        FileVersion target = versionRepository.findByFileIdAndLabel(file.getId(), label)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Version %s does not exist for %s".formatted(label, file.getFileCode())));

        versionRepository.findFirstByFileIdAndIsCurrentTrue(file.getId()).ifPresent(current -> {
            // Flush the clear before inserting the replacement: uq_file_versions_current
            // is a partial unique index on (file_id) WHERE is_current, and Hibernate is
            // free to order the INSERT before the UPDATE within one transaction.
            current.setCurrent(false);
            versionRepository.saveAndFlush(current);
        });

        target.setCurrent(true);
        versionRepository.save(target);

        file.setCurrentVersionId(target.getId());
        file.setSizeBytes(target.getSizeBytes());
        file.setChecksumSha256(target.getChecksumSha256());
        file.setMimeType(target.getMimeType());
        fileRepository.save(file);

        auditService.record("file", file.getId(), FileAuditService.ACTION_ROLLED_BACK, null,
                Map.of("version", label));
        publish(FileDomainEvent.VERSION_ROLLED_BACK, file, Map.of("version", label));
        return target;
    }

    // ---- Download -----------------------------------------------------------

    /**
     * Streams a file's content.
     *
     * <p>Download is gated on {@code allowsDownload} of the configured status, so
     * quarantining a file makes it unservable without any call site testing for
     * the string "quarantined".
     */
    @Transactional
    public Download download(UUID fileUuid, String versionLabel) {
        ManagedFile file = requireFile(fileUuid);
        if (!file.getStatus().isAllowsDownload()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A file in status '%s' cannot be downloaded".formatted(file.getStatus().getCode()));
        }

        FileVersion version = versionLabel == null || versionLabel.isBlank()
                ? versionRepository.findFirstByFileIdAndIsCurrentTrue(file.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                            "File %s has no current version".formatted(file.getFileCode())))
                : versionRepository.findByFileIdAndLabel(file.getId(), versionLabel)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Version %s does not exist".formatted(versionLabel)));

        FileContent content = readVersion(version);
        auditService.record("file", file.getId(), FileAuditService.ACTION_DOWNLOADED, null,
                Map.of("version", version.getLabel()));
        publish(FileDomainEvent.FILE_DOWNLOADED, file, Map.of("version", version.getLabel()));
        return new Download(file, version, content);
    }

    /** Streams a derivative (thumbnail, preview, watermarked copy) of a version. */
    @Transactional(readOnly = true)
    public Download derivative(UUID fileUuid, String kind) {
        ManagedFile file = requireFile(fileUuid);
        if (!file.getStatus().isAllowsDownload()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A file in status '%s' cannot be downloaded".formatted(file.getStatus().getCode()));
        }
        FileVersion version = versionRepository.findFirstByFileIdAndIsCurrentTrue(file.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "No current version"));
        FileDerivative derivative = derivativeRepository
                .findByVersionIdAndKind(version.getId(), kind)
                .orElseThrow(() -> ResourceNotFoundException.of("Derivative", kind));

        if (derivative.getStorageKey() == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Derivative '%s' has no stored content".formatted(kind));
        }
        StorageProviderConfig provider = derivative.getProvider();
        StorageProviderHandler handler = validationService.requireAvailable(provider);
        try {
            FileContent content = handler.load(derivative.getStorageKey(), provider.getConfig())
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Derivative content is missing from storage"));
            return new Download(file, version, content);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not read derivative: " + e.getMessage());
        }
    }

    // ---- Lifecycle ----------------------------------------------------------

    /**
     * Soft-deletes. Refuses while live references exist — deleting a file that a
     * repair record or installment contract still points at would leave a
     * dangling reference the business module cannot detect.
     */
    @Transactional
    public ManagedFile delete(UUID fileUuid, boolean force) {
        ManagedFile file = requireFile(fileUuid);

        if (file.isUnderLegalHold()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "File is under legal hold until " + file.getLegalHoldUntil());
        }
        long references = referenceRepository.countByFileId(file.getId());
        if (references > 0 && !force) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "File is referenced by %d record(s) and cannot be deleted".formatted(references));
        }

        FileStatus deleted = statusRepository.findFirstByIsDeletedStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No file status is flagged as the deleted state"));

        Map<String, Object> before = Map.of("status", file.getStatus().getCode());
        file.setStatus(deleted);
        file.setDeletedAt(Instant.now());
        file.setDeletedBy(CurrentActor.id());
        fileRepository.save(file);

        auditService.record("file", file.getId(), FileAuditService.ACTION_DELETED, before,
                Map.of("status", deleted.getCode(), "forced", force));
        publish(FileDomainEvent.FILE_DELETED, file, Map.of("forced", force));
        return file;
    }

    @Transactional
    public ManagedFile restore(UUID fileUuid) {
        ManagedFile file = requireFile(fileUuid);
        if (!file.isDeleted()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "File is not deleted");
        }
        FileStatus available = statusRepository.findFirstByIsAvailableStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No file status is flagged as the available state"));

        file.setStatus(available);
        file.setDeletedAt(null);
        file.setDeletedBy(null);
        file.setRestoredAt(Instant.now());
        fileRepository.save(file);

        auditService.record("file", file.getId(), FileAuditService.ACTION_RESTORED, null,
                Map.of("status", available.getCode()));
        publish(FileDomainEvent.FILE_RESTORED, file, Map.of());
        return file;
    }

    @Transactional
    public ManagedFile move(UUID fileUuid, Long folderId) {
        ManagedFile file = requireFile(fileUuid);
        Map<String, Object> before = Map.of("folderId",
                file.getFolder() == null ? "null" : file.getFolder().getId());
        file.setFolder(folderId == null ? null : folderRepository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Folder", folderId)));
        fileRepository.save(file);
        auditService.record("file", file.getId(), FileAuditService.ACTION_MOVED, before,
                Map.of("folderId", folderId == null ? "null" : folderId));
        return file;
    }

    /**
     * Copies metadata to a new file that shares the source's stored bytes.
     * Deliberately does not duplicate the payload — the version rows point at the
     * same storage key, which is safe because stored objects are immutable.
     */
    @Transactional
    public ManagedFile copy(UUID fileUuid, String newName, Long folderId) {
        ManagedFile source = requireFile(fileUuid);
        FileVersion sourceVersion = versionRepository
                .findFirstByFileIdAndIsCurrentTrue(source.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "No current version"));

        ManagedFile copy = ManagedFile.builder()
                .fileUuid(UUID.randomUUID())
                .fileCode(nextFileCode())
                .name(newName == null || newName.isBlank() ? source.getName() + " (copy)" : newName)
                .originalFilename(source.getOriginalFilename())
                .description(source.getDescription())
                .category(source.getCategory())
                .status(source.getStatus())
                .folder(folderId == null ? source.getFolder() : folderRepository.findById(folderId)
                        .orElseThrow(() -> ResourceNotFoundException.of("Folder", folderId)))
                .moduleCode(source.getModuleCode())
                .entityCode(source.getEntityCode())
                .entityId(source.getEntityId())
                .companyId(source.getCompanyId())
                .branchId(source.getBranchId())
                .ownerId(CurrentActor.id())
                .ownerEmail(CurrentActor.email())
                .sizeBytes(source.getSizeBytes())
                .extension(source.getExtension())
                .mimeType(source.getMimeType())
                // Checksum is deliberately NOT copied: the dedupe index is unique
                // per (tenant, category, checksum) among live files, and a copy is
                // an intentional second row.
                .checksumSha256(null)
                .metadata(new HashMap<>(source.getMetadata()))
                .retentionPolicy(source.getRetentionPolicy())
                .tenantId(source.getTenantId())
                .build();
        copy = fileRepository.save(copy);

        FileVersion version = FileVersion.builder()
                .fileId(copy.getId())
                .versionMajor(1).versionMinor(0).revision(0)
                .label(FileVersion.label(1, 0, 0))
                .provider(sourceVersion.getProvider())
                .storageKey(sourceVersion.getStorageKey())
                .sizeBytes(sourceVersion.getSizeBytes())
                .checksumSha256(sourceVersion.getChecksumSha256())
                .mimeType(sourceVersion.getMimeType())
                .comment("Copied from " + source.getFileCode())
                .isCurrent(true)
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .tenantId(copy.getTenantId())
                .build();
        version = versionRepository.save(version);
        copy.setCurrentVersionId(version.getId());
        fileRepository.save(copy);

        auditService.record("file", copy.getId(), FileAuditService.ACTION_COPIED,
                Map.of("sourceFile", source.getFileCode()), Map.of("file", copy.getFileCode()));
        return copy;
    }

    // ---- References ---------------------------------------------------------

    /** Registers that a business record points at this file. */
    @Transactional
    public FileReference addReference(UUID fileUuid, String moduleCode, String entityCode,
                                      Long recordId, String role) {
        ManagedFile file = requireFile(fileUuid);
        return referenceRepository.save(FileReference.builder()
                .fileId(file.getId())
                .moduleCode(moduleCode)
                .entityCode(entityCode)
                .recordId(recordId)
                .role(role)
                .tenantId(file.getTenantId())
                .build());
    }

    @Transactional(readOnly = true)
    public List<FileReference> references(UUID fileUuid) {
        return referenceRepository.findAllByFileId(requireFile(fileUuid).getId());
    }

    @Transactional(readOnly = true)
    public List<ManagedFile> forRecord(String moduleCode, String entityCode, Long recordId) {
        return fileRepository.findAllByModuleCodeAndEntityCodeAndEntityIdAndDeletedAtIsNull(
                moduleCode, entityCode, recordId);
    }

    @Transactional(readOnly = true)
    public List<FileVersion> versions(UUID fileUuid) {
        return versionRepository
                .findAllByFileIdOrderByVersionMajorDescVersionMinorDescRevisionDesc(
                        requireFile(fileUuid).getId());
    }

    @Transactional(readOnly = true)
    public ManagedFile requireFile(UUID fileUuid) {
        return fileRepository.findByFileUuid(fileUuid)
                .orElseThrow(() -> ResourceNotFoundException.of("File", fileUuid));
    }

    // ---- Internals ----------------------------------------------------------

    private ManagedFile newFile(UploadCommand command, FileCategory category, Staged staged) {
        RetentionPolicy retention = category.getRetentionPolicy();
        Instant expiresAt = retention != null && retention.getRetainDays() != null
                ? Instant.now().plusSeconds(retention.getRetainDays() * 86_400L)
                : null;

        FileStatus processing = statusRepository.findFirstByIsProcessingStateTrue()
                .orElseGet(() -> statusRepository.findFirstByIsDefaultTrue()
                        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                                "No default file status is configured")));

        return ManagedFile.builder()
                .fileUuid(UUID.randomUUID())
                .fileCode(nextFileCode())
                .name(command.name() == null || command.name().isBlank()
                        ? command.filename() : command.name())
                .originalFilename(command.filename())
                .description(command.description())
                .category(category)
                .status(processing)
                .folder(command.folderId() == null ? null : folderRepository.findById(command.folderId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Folder", command.folderId())))
                .moduleCode(command.moduleCode())
                .entityCode(command.entityCode())
                .entityId(command.entityId())
                .companyId(command.companyId())
                .branchId(command.branchId())
                .ownerId(CurrentActor.id())
                .ownerEmail(CurrentActor.email())
                .sizeBytes(staged.size())
                .extension(FileValidationService.extensionOf(command.filename()))
                .mimeType(staged.contentType())
                .checksumSha256(staged.checksum())
                .metadata(new HashMap<>())
                .retentionPolicy(retention)
                .retentionExpiresAt(expiresAt)
                .uploadedIp(FileAuditService.clientIp())
                .tenantId(resolveTenantId())
                .build();
    }

    private FileVersion createVersion(ManagedFile file, StorageProviderConfig provider,
                                      String storageKey, Staged staged,
                                      int major, int minor, int revision, String comment) {
        return versionRepository.save(FileVersion.builder()
                .fileId(file.getId())
                .versionMajor(major)
                .versionMinor(minor)
                .revision(revision)
                .label(FileVersion.label(major, minor, revision))
                .provider(provider)
                .storageKey(storageKey)
                .sizeBytes(staged.size())
                .checksumSha256(staged.checksum())
                .mimeType(staged.contentType())
                .comment(comment)
                .isCurrent(true)
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .tenantId(file.getTenantId())
                .build());
    }

    /**
     * Runs the category's configured pipeline. A processor whose bean is missing
     * is skipped with a warning rather than failing the upload — except when the
     * row is marked {@code failsUpload}, which is how a required virus scan
     * refuses to be silently bypassed.
     */
    private void runProcessors(ManagedFile file, FileVersion version,
                               FileCategory category, Staged staged) {
        List<String> codes = category.getProcessors();
        if (codes == null || codes.isEmpty()) {
            return;
        }
        List<FileProcessorConfig> configs =
                processorConfigRepository.findAllByCodeInAndEnabledTrueOrderByRunOrderAsc(codes);

        for (FileProcessorConfig config : configs) {
            Optional<FileProcessor> processor = processorRegistry.find(config.getHandlerKey());
            if (processor.isEmpty()) {
                if (config.isFailsUpload()) {
                    throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                            "Processor '%s' is enabled and required but has no implementation"
                                    .formatted(config.getCode()));
                }
                log.warn("Processor '{}' is enabled but no bean is registered for handler '{}'",
                        config.getCode(), config.getHandlerKey());
                continue;
            }
            FileProcessor bean = processor.get();
            if (!bean.supports(staged.contentType())) {
                continue;
            }
            try {
                FileProcessor.Result result = bean.process(staged.content(), config.getConfig());
                applyResult(file, version, config, result);
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                if (config.isFailsUpload()) {
                    throw new ApiException(ErrorCode.INTERNAL_ERROR,
                            "Required processor '%s' failed: %s".formatted(config.getCode(), e.getMessage()));
                }
                log.warn("Processor '{}' failed for file {}: {}",
                        config.getCode(), file.getFileCode(), e.toString());
            }
        }
    }

    private void applyResult(ManagedFile file, FileVersion version,
                             FileProcessorConfig config, FileProcessor.Result result) {
        if (result == null) {
            return;
        }
        if (result.metadata() != null && !result.metadata().isEmpty()) {
            Map<String, Object> merged = new HashMap<>(file.getMetadata());
            merged.putAll(result.metadata());
            file.setMetadata(merged);
        }

        if (result.verdict() != null) {
            scanRepository.save(FileScanResult.builder()
                    .fileId(file.getId())
                    .versionId(version.getId())
                    .scannerKey(config.getHandlerKey())
                    .verdict(result.verdict().verdict())
                    .threat(result.verdict().threat())
                    .details(result.verdict().details() == null ? Map.of() : result.verdict().details())
                    .tenantId(file.getTenantId())
                    .build());

            if (result.verdict().infected()) {
                quarantine(file, result.verdict().threat());
            }
        }

        for (FileProcessor.Derivative derivative : result.derivatives()) {
            storeDerivative(file, version, config, derivative);
        }
    }

    private void storeDerivative(ManagedFile file, FileVersion version,
                                 FileProcessorConfig config, FileProcessor.Derivative derivative) {
        String storageKey = null;
        StorageProviderConfig provider = version.getProvider();

        if (derivative.content() != null) {
            try {
                StorageProviderHandler handler = validationService.requireAvailable(provider);
                storageKey = handler.store(file.getCategory().getCode() + "/derivatives",
                        derivative.content(), provider.getConfig());
            } catch (IOException e) {
                log.warn("Could not store {} derivative for {}: {}",
                        derivative.kind(), file.getFileCode(), e.toString());
                return;
            }
        }

        derivativeRepository.save(FileDerivative.builder()
                .fileId(file.getId())
                .versionId(version.getId())
                .kind(derivative.kind())
                .provider(storageKey == null ? null : provider)
                .storageKey(storageKey)
                .mimeType(derivative.content() == null ? null : derivative.content().contentType())
                .sizeBytes(derivative.content() == null ? null : derivative.content().size())
                .width(derivative.width())
                .height(derivative.height())
                .pageCount(derivative.pageCount())
                .durationMs(derivative.durationMs())
                .textContent(derivative.text())
                .generatorKey(config.getHandlerKey())
                .status("ready")
                .generatedAt(Instant.now())
                .tenantId(file.getTenantId())
                .build());

        publish("thumbnail".equals(derivative.kind())
                        ? FileDomainEvent.THUMBNAIL_GENERATED
                        : FileDomainEvent.PREVIEW_GENERATED,
                file, Map.of("kind", derivative.kind()));
    }

    private void quarantine(ManagedFile file, String threat) {
        statusRepository.findFirstByIsQuarantinedStateTrue().ifPresent(file::setStatus);
        auditService.record("file", file.getId(), FileAuditService.ACTION_QUARANTINED, null,
                Map.of("threat", threat == null ? "unknown" : threat));
        publish(FileDomainEvent.VIRUS_DETECTED, file,
                Map.of("threat", threat == null ? "unknown" : threat));
        publish(FileDomainEvent.FILE_QUARANTINED, file, Map.of());
    }

    /** A quarantined file must not be flipped back to available by the pipeline. */
    private FileStatus resolveStatusAfterProcessing(ManagedFile file) {
        if (file.getStatus() != null && file.getStatus().isQuarantinedState()) {
            return file.getStatus();
        }
        return statusRepository.findFirstByIsAvailableStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No file status is flagged as the available state"));
    }

    private StorageProviderConfig resolveProvider(FileCategory category) {
        if (category.getDefaultProvider() != null && category.getDefaultProvider().isEnabled()) {
            return category.getDefaultProvider();
        }
        return providerRepository.findFirstByIsDefaultTrueAndEnabledTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No enabled default storage provider is configured"));
    }

    private String store(StorageProviderHandler handler, StorageProviderConfig provider,
                         String keyHint, Staged staged) {
        try {
            return handler.store(keyHint, staged.content(), provider.getConfig());
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "Storage provider %s could not store the file: %s"
                            .formatted(provider.getCode(), e.getMessage()));
        }
    }

    private FileContent readVersion(FileVersion version) {
        StorageProviderConfig provider = version.getProvider();
        StorageProviderHandler handler = validationService.requireAvailable(provider);
        try {
            FileContent stored = handler.load(version.getStorageKey(), provider.getConfig())
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                            "File content is missing from storage"));
            // The provider knows bytes, not semantics: re-attach the recorded
            // content type rather than trusting a filename extension.
            return new FileContent() {
                @Override
                public InputStream openStream() throws IOException {
                    return stored.openStream();
                }

                @Override
                public long size() {
                    return version.getSizeBytes();
                }

                @Override
                public String contentType() {
                    return version.getMimeType();
                }

                @Override
                public String filename() {
                    return stored.filename();
                }
            };
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not read file: " + e.getMessage());
        }
    }

    private void addReferenceIfRequested(ManagedFile file, UploadCommand command) {
        if (command.moduleCode() == null || command.entityCode() == null || command.entityId() == null) {
            return;
        }
        boolean exists = referenceRepository.findAllByFileId(file.getId()).stream()
                .anyMatch(r -> r.getModuleCode().equals(command.moduleCode())
                        && r.getEntityCode().equals(command.entityCode())
                        && r.getRecordId().equals(command.entityId()));
        if (!exists) {
            referenceRepository.save(FileReference.builder()
                    .fileId(file.getId())
                    .moduleCode(command.moduleCode())
                    .entityCode(command.entityCode())
                    .recordId(command.entityId())
                    .role(command.role())
                    .tenantId(file.getTenantId())
                    .build());
        }
    }

    private String nextFileCode() {
        return "FILE-%08d".formatted(fileRepository.nextCodeSequence());
    }

    /**
     * Until {@code TenantContext} exists, the DEFAULT tenant is resolved
     * explicitly. Returning null does NOT work: Hibernate includes tenant_id in
     * the INSERT, and an explicit NULL overrides the column DEFAULT rather than
     * triggering it. When enforcement lands this becomes
     * {@code TenantContext.require()}.
     */
    private Long resolveTenantId() {
        return tenantDefaults.current();
    }

    private void publish(String eventType, ManagedFile file, Map<String, Object> payload) {
        events.publishEvent(FileDomainEvent.of(eventType, file.getFileUuid(), file.getId(),
                file.getModuleCode(), file.getEntityCode(), file.getEntityId(), payload));
    }

    /**
     * Stages the upload to a temp file while computing its SHA-256 in one pass.
     * Re-readable, so validation, dedupe, storage and every processor can each
     * stream it without buffering the payload in memory.
     */
    private Staged stage(InputStream source, String filename, String contentType) {
        Path stagingDir = Path.of(properties.stagingPath()).toAbsolutePath().normalize();
        Path temp;
        try {
            Files.createDirectories(stagingDir);
            temp = Files.createTempFile(stagingDir, "upload-", ".tmp");
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload staging file", e);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream in = new DigestInputStream(source, digest)) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            String checksum = HexFormat.of().formatHex(digest.digest());
            long size = Files.size(temp);
            return new Staged(temp, size, checksum, contentType, filename);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // best effort
            }
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "The upload was interrupted: " + e.getMessage());
        }
    }

    /** A staged upload on local disk, deleted once the transaction is done with it. */
    private record Staged(Path path, long size, String checksum, String contentType, String filename) {

        FileContent content() {
            return FileContents.ofPath(path, contentType, filename);
        }

        void discard() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                // A leaked temp file is reclaimed by the retention sweep.
            }
        }
    }

    /** What a business module supplies to store a file. */
    public record UploadCommand(String categoryCode,
                                String filename,
                                String contentType,
                                String name,
                                String description,
                                Long folderId,
                                String moduleCode,
                                String entityCode,
                                Long entityId,
                                String role,
                                Long companyId,
                                Long branchId,
                                String declaredChecksum) {
    }

    /** A streamable download plus the metadata the transport layer needs. */
    public record Download(ManagedFile file, FileVersion version, FileContent content) {
    }
}
