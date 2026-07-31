package com.sami.app.files.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.files.FilesProperties;
import com.sami.app.files.domain.ManagedFile;
import com.sami.app.files.dto.FileDtos.AddReferenceRequest;
import com.sami.app.files.dto.FileDtos.AssignTagsRequest;
import com.sami.app.files.dto.FileDtos.CatalogResponse;
import com.sami.app.files.dto.FileDtos.CategoryResponse;
import com.sami.app.files.dto.FileDtos.DerivativeResponse;
import com.sami.app.files.dto.FileDtos.FileResponse;
import com.sami.app.files.dto.FileDtos.FolderRequest;
import com.sami.app.files.dto.FileDtos.FolderResponse;
import com.sami.app.files.dto.FileDtos.ProcessorResponse;
import com.sami.app.files.dto.FileDtos.ProviderResponse;
import com.sami.app.files.dto.FileDtos.ReferenceResponse;
import com.sami.app.files.dto.FileDtos.RenameFolderRequest;
import com.sami.app.files.dto.FileDtos.RetentionPolicyResponse;
import com.sami.app.files.dto.FileDtos.ScanResultResponse;
import com.sami.app.files.dto.FileDtos.StatusResponse;
import com.sami.app.files.dto.FileDtos.TagResponse;
import com.sami.app.files.dto.FileDtos.UpdateFileRequest;
import com.sami.app.files.dto.FileDtos.VersionResponse;
import com.sami.app.files.repository.FileCategoryRepository;
import com.sami.app.files.repository.FileDerivativeRepository;
import com.sami.app.files.repository.FileProcessorConfigRepository;
import com.sami.app.files.repository.FileScanResultRepository;
import com.sami.app.files.repository.FileStatusRepository;
import com.sami.app.files.repository.FileVersionRepository;
import com.sami.app.files.repository.ManagedFileRepository;
import com.sami.app.files.repository.ManagedFileSpecifications;
import com.sami.app.files.repository.RetentionPolicyRepository;
import com.sami.app.files.repository.StorageProviderConfigRepository;
import com.sami.app.files.service.FileReportService;
import com.sami.app.files.service.FileService;
import com.sami.app.files.service.FileTagService;
import com.sami.app.files.service.FolderService;
import com.sami.app.files.service.RetentionService;
import com.sami.app.files.spi.FileProcessorRegistry;
import com.sami.app.files.spi.StorageProviderRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST surface for the files module.
 *
 * <p>Files are addressed by {@code fileUuid} throughout — the database id and the
 * storage key are never exposed, so no client can be written against them.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files & Media", description = "Central file storage, versions, derivatives and retention")
public class FileController {

    private final FileService fileService;
    private final FolderService folderService;
    private final FileTagService tagService;
    private final RetentionService retentionService;
    private final FileReportService reportService;
    private final ManagedFileRepository fileRepository;
    private final FileVersionRepository versionRepository;
    private final FileDerivativeRepository derivativeRepository;
    private final FileScanResultRepository scanRepository;
    private final FileCategoryRepository categoryRepository;
    private final FileStatusRepository statusRepository;
    private final StorageProviderConfigRepository providerRepository;
    private final FileProcessorConfigRepository processorConfigRepository;
    private final RetentionPolicyRepository retentionPolicyRepository;
    private final StorageProviderRegistry storageRegistry;
    private final FileProcessorRegistry processorRegistry;
    private final FilesProperties properties;

    // ---- Upload / download --------------------------------------------------

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('files:upload')")
    @Operation(summary = "Upload a file")
    public ApiResponse<FileResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("category") String categoryCode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String entityCode,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String checksum) {

        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A file is required");
        }
        FileService.UploadCommand command = new FileService.UploadCommand(
                categoryCode, file.getOriginalFilename(), file.getContentType(),
                name, description, folderId, moduleCode, entityCode, entityId, role,
                companyId, branchId, checksum);

        try (var in = file.getInputStream()) {
            ManagedFile stored = fileService.upload(command, in);
            return ApiResponse.ok(toResponse(stored));
        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Could not read the upload: " + e.getMessage());
        }
    }

    @PostMapping(value = "/{fileUuid}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('files:upload')")
    @Operation(summary = "Add a new version of a file")
    public ApiResponse<VersionResponse> addVersion(@PathVariable UUID fileUuid,
                                                   @RequestPart("file") MultipartFile file,
                                                   @RequestParam(defaultValue = "false") boolean major,
                                                   @RequestParam(required = false) String comment) {
        try (var in = file.getInputStream()) {
            return ApiResponse.ok(VersionResponse.from(fileService.addVersion(
                    fileUuid, in, file.getOriginalFilename(), file.getContentType(), major, comment)));
        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Could not read the upload: " + e.getMessage());
        }
    }

    @GetMapping("/{fileUuid}/content")
    @PreAuthorize("@authz.has('files:download')")
    @Operation(summary = "Download file content")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID fileUuid,
                                                        @RequestParam(required = false) String version) {
        FileService.Download download = fileService.download(fileUuid, version);
        return stream(download, download.file().getOriginalFilename());
    }

    @GetMapping("/{fileUuid}/derivatives/{kind}")
    @PreAuthorize("@authz.has('files:download')")
    @Operation(summary = "Download a derivative (thumbnail, preview, …)")
    public ResponseEntity<InputStreamResource> derivative(@PathVariable UUID fileUuid,
                                                          @PathVariable String kind) {
        FileService.Download download = fileService.derivative(fileUuid, kind);
        return stream(download, kind + "-" + download.file().getOriginalFilename());
    }

    private ResponseEntity<InputStreamResource> stream(FileService.Download download, String filename) {
        try {
            var content = download.content();
            String contentType = content.contentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM_VALUE : content.contentType();
            String encoded = URLEncoder.encode(filename == null ? "download" : filename,
                    StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encoded)
                    .contentLength(content.size())
                    .body(new InputStreamResource(content.openStream()));
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not stream the file");
        }
    }

    // ---- Search / metadata --------------------------------------------------

    @GetMapping
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "Search files with combined filters")
    public ApiResponse<PageResponse<FileResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String entityCode,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20) Pageable pageable) {

        Specification<ManagedFile> spec = Specification.allOf(
                ManagedFileSpecifications.nameContains(q),
                ManagedFileSpecifications.categoryCode(category),
                ManagedFileSpecifications.statusCode(status),
                ManagedFileSpecifications.module(moduleCode),
                ManagedFileSpecifications.entity(entityCode, entityId),
                ManagedFileSpecifications.owner(ownerId),
                ManagedFileSpecifications.folder(folderId),
                ManagedFileSpecifications.company(companyId),
                ManagedFileSpecifications.branch(branchId),
                ManagedFileSpecifications.sizeBetween(minSize, maxSize),
                ManagedFileSpecifications.uploadedBetween(from, to),
                ManagedFileSpecifications.deleted(includeDeleted));

        // Tag filtering resolves to ids first: the link table is a separate
        // aggregate, and joining it into the Specification would duplicate rows.
        if (tagIds != null && !tagIds.isEmpty()) {
            spec = spec.and(ManagedFileSpecifications.idIn(tagService.fileIdsWithTags(tagIds)));
        }

        Page<ManagedFile> page = fileRepository.findAll(spec, pageable);
        return ApiResponse.ok(PageResponse.from(page, this::toResponse));
    }

    @GetMapping("/{fileUuid}")
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "Get file metadata")
    public ApiResponse<FileResponse> get(@PathVariable UUID fileUuid) {
        return ApiResponse.ok(toResponse(fileService.requireFile(fileUuid)));
    }

    @PutMapping("/{fileUuid}")
    @PreAuthorize("@authz.has('files:edit')")
    @Operation(summary = "Update file metadata")
    public ApiResponse<FileResponse> update(@PathVariable UUID fileUuid,
                                            @Valid @RequestBody UpdateFileRequest request) {
        ManagedFile file = fileService.requireFile(fileUuid);
        if (request.name() != null && !request.name().isBlank()) {
            file.setName(request.name());
        }
        if (request.description() != null) {
            file.setDescription(request.description());
        }
        if (request.legalHoldUntil() != null) {
            file.setLegalHoldUntil(request.legalHoldUntil());
        }
        if (request.retentionPolicyCode() != null) {
            file.setRetentionPolicy(retentionPolicyRepository.findByCode(request.retentionPolicyCode())
                    .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                            "Unknown retention policy: " + request.retentionPolicyCode())));
        }
        fileRepository.save(file);
        if (request.folderId() != null) {
            fileService.move(fileUuid, request.folderId());
        }
        return ApiResponse.ok(toResponse(fileService.requireFile(fileUuid)));
    }

    @GetMapping("/{fileUuid}/versions")
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "List file versions")
    public ApiResponse<List<VersionResponse>> versions(@PathVariable UUID fileUuid) {
        return ApiResponse.ok(fileService.versions(fileUuid).stream()
                .map(VersionResponse::from).toList());
    }

    @PostMapping("/{fileUuid}/versions/{label}/rollback")
    @PreAuthorize("@authz.has('files:edit')")
    @Operation(summary = "Roll back to an earlier version")
    public ApiResponse<VersionResponse> rollback(@PathVariable UUID fileUuid,
                                                 @PathVariable String label) {
        return ApiResponse.ok(VersionResponse.from(fileService.rollback(fileUuid, label)));
    }

    @GetMapping("/{fileUuid}/derivatives")
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "List generated derivatives")
    public ApiResponse<List<DerivativeResponse>> derivatives(@PathVariable UUID fileUuid) {
        ManagedFile file = fileService.requireFile(fileUuid);
        return ApiResponse.ok(derivativeRepository.findAllByFileId(file.getId()).stream()
                .map(DerivativeResponse::from).toList());
    }

    @GetMapping("/{fileUuid}/scans")
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "List scan results")
    public ApiResponse<List<ScanResultResponse>> scans(@PathVariable UUID fileUuid) {
        ManagedFile file = fileService.requireFile(fileUuid);
        return ApiResponse.ok(scanRepository.findAllByFileIdOrderByScannedAtDesc(file.getId())
                .stream().map(ScanResultResponse::from).toList());
    }

    // ---- Lifecycle ----------------------------------------------------------

    @DeleteMapping("/{fileUuid}")
    @PreAuthorize("@authz.has('files:delete')")
    @Operation(summary = "Soft-delete a file")
    public ApiResponse<FileResponse> delete(@PathVariable UUID fileUuid,
                                            @RequestParam(defaultValue = "false") boolean force) {
        return ApiResponse.ok(toResponse(fileService.delete(fileUuid, force)));
    }

    @PostMapping("/{fileUuid}/restore")
    @PreAuthorize("@authz.has('files:restore')")
    @Operation(summary = "Restore a deleted file")
    public ApiResponse<FileResponse> restore(@PathVariable UUID fileUuid) {
        return ApiResponse.ok(toResponse(fileService.restore(fileUuid)));
    }

    @PostMapping("/{fileUuid}/copy")
    @PreAuthorize("@authz.has('files:create') or @authz.has('files:upload')")
    @Operation(summary = "Copy a file")
    public ApiResponse<FileResponse> copy(@PathVariable UUID fileUuid,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) Long folderId) {
        return ApiResponse.ok(toResponse(fileService.copy(fileUuid, name, folderId)));
    }

    // ---- References & tags --------------------------------------------------

    @GetMapping("/{fileUuid}/references")
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "List business records referencing this file")
    public ApiResponse<List<ReferenceResponse>> references(@PathVariable UUID fileUuid) {
        return ApiResponse.ok(fileService.references(fileUuid).stream()
                .map(ReferenceResponse::from).toList());
    }

    @PostMapping("/{fileUuid}/references")
    @PreAuthorize("@authz.has('files:edit')")
    @Operation(summary = "Register a reference from a business record")
    public ApiResponse<ReferenceResponse> addReference(@PathVariable UUID fileUuid,
                                                       @Valid @RequestBody AddReferenceRequest request) {
        return ApiResponse.ok(ReferenceResponse.from(fileService.addReference(
                fileUuid, request.moduleCode(), request.entityCode(),
                request.recordId(), request.role())));
    }

    @GetMapping("/for-record")
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "List files attached to a business record")
    public ApiResponse<List<FileResponse>> forRecord(@RequestParam String moduleCode,
                                                     @RequestParam String entityCode,
                                                     @RequestParam Long recordId) {
        return ApiResponse.ok(fileService.forRecord(moduleCode, entityCode, recordId).stream()
                .map(this::toResponse).toList());
    }

    @PutMapping("/{fileUuid}/tags")
    @PreAuthorize("@authz.has('files:edit')")
    @Operation(summary = "Replace a file's tags")
    public ApiResponse<List<TagResponse>> assignTags(@PathVariable UUID fileUuid,
                                                     @Valid @RequestBody AssignTagsRequest request) {
        return ApiResponse.ok(tagService.assign(fileUuid, request.tagIds()).stream()
                .map(TagResponse::from).toList());
    }

    // ---- Folders ------------------------------------------------------------

    @GetMapping("/folders")
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "List folders")
    public ApiResponse<List<FolderResponse>> folders(@RequestParam(required = false) Long parentId) {
        var folders = parentId == null ? folderService.roots() : folderService.children(parentId);
        return ApiResponse.ok(folders.stream().map(FolderResponse::from).toList());
    }

    @PostMapping("/folders")
    @PreAuthorize("@authz.has('files:edit')")
    @Operation(summary = "Create a folder")
    public ApiResponse<FolderResponse> createFolder(@Valid @RequestBody FolderRequest request) {
        return ApiResponse.ok(FolderResponse.from(folderService.create(
                request.parentId(), request.name(), request.description(),
                request.virtual(), request.smart(), request.smartQuery())));
    }

    @PatchMapping("/folders/{id}")
    @PreAuthorize("@authz.has('files:edit')")
    @Operation(summary = "Rename a folder")
    public ApiResponse<FolderResponse> renameFolder(@PathVariable Long id,
                                                    @Valid @RequestBody RenameFolderRequest request) {
        return ApiResponse.ok(FolderResponse.from(folderService.rename(id, request.name())));
    }

    @DeleteMapping("/folders/{id}")
    @PreAuthorize("@authz.has('files:delete')")
    @Operation(summary = "Delete an empty folder")
    public ApiResponse<Void> deleteFolder(@PathVariable Long id) {
        folderService.delete(id);
        return ApiResponse.ok();
    }

    // ---- Retention & reports ------------------------------------------------

    @PostMapping("/retention/apply")
    @PreAuthorize("@authz.has('files:manage-retention')")
    @Operation(summary = "Apply expired retention policies")
    public ApiResponse<RetentionService.Summary> applyRetention() {
        return ApiResponse.ok(retentionService.applyExpired());
    }

    @GetMapping("/reports/{report}")
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "Run a storage report")
    public ApiResponse<List<Map<String, Object>>> report(@PathVariable String report) {
        return ApiResponse.ok(reportService.run(report));
    }

    @GetMapping(value = "/reports/{report}/export.csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("@authz.has('files:export')")
    @Operation(summary = "Export a storage report as CSV")
    public ResponseEntity<String> exportReport(@PathVariable String report) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + report + ".csv\"")
                .body(reportService.toCsv(reportService.run(report)));
    }

    // ---- Catalog ------------------------------------------------------------

    @GetMapping("/catalog")
    @PreAuthorize("@authz.has('files:view')")
    @Operation(summary = "Configuration catalogue and registered plugin beans")
    public ApiResponse<CatalogResponse> catalog() {
        List<String> storageKeys = storageRegistry.keys();
        List<String> processorKeys = processorRegistry.keys();

        return ApiResponse.ok(new CatalogResponse(
                categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(CategoryResponse::from).toList(),
                statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(StatusResponse::from).toList(),
                providerRepository.findAllByOrderByPriorityAsc().stream()
                        .map(p -> ProviderResponse.from(p,
                                storageKeys.contains(p.getHandlerKey()),
                                storageRegistry.find(p.getHandlerKey())
                                        .map(h -> h.available(p.getConfig())).orElse(false)))
                        .toList(),
                processorConfigRepository.findAllByOrderByRunOrderAsc().stream()
                        .map(p -> ProcessorResponse.from(p, processorKeys.contains(p.getHandlerKey())))
                        .toList(),
                tagService.all().stream().map(TagResponse::from).toList(),
                retentionPolicyRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(RetentionPolicyResponse::from).toList(),
                storageKeys,
                processorKeys,
                List.of("delete", "archive", "anonymise", "notify"),
                reportService.available()));
    }

    private FileResponse toResponse(ManagedFile file) {
        String label = file.getCurrentVersionId() == null ? null
                : versionRepository.findById(file.getCurrentVersionId())
                        .map(v -> v.getLabel()).orElse(null);
        return FileResponse.from(file, label);
    }
}
