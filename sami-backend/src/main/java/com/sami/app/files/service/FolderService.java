package com.sami.app.files.service;

import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.files.domain.FileFolder;
import com.sami.app.files.repository.FileFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Folder tree over a materialised path, so a subtree query is a prefix match
 * rather than a recursive walk. Virtual and smart folders share the tree but
 * hold no files of their own.
 */
@Service
@RequiredArgsConstructor
public class FolderService {

    private final FileFolderRepository repository;
    private final FileAuditService auditService;
    private final TenantDefaults tenantDefaults;

    @Transactional(readOnly = true)
    public List<FileFolder> roots() {
        return repository.findAllByParentIsNullOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<FileFolder> children(Long parentId) {
        return repository.findAllByParentIdOrderByNameAsc(parentId);
    }

    @Transactional(readOnly = true)
    public FileFolder get(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Folder", id));
    }

    @Transactional
    public FileFolder create(Long parentId, String name, String description,
                             boolean virtual, boolean smart, Map<String, Object> smartQuery) {
        if (name == null || name.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A folder name is required");
        }
        if (smart && (smartQuery == null || smartQuery.isEmpty())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "A smart folder requires a query definition");
        }

        FileFolder parent = parentId == null ? null : get(parentId);
        String path = buildPath(parent, name);

        if (repository.findByPath(path).isPresent()) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A folder already exists at " + path);
        }

        FileFolder folder = repository.save(FileFolder.builder()
                .parent(parent)
                .name(name)
                .path(path)
                .description(description)
                .isVirtual(virtual)
                .isSmart(smart)
                .smartQuery(smartQuery == null ? Map.of() : smartQuery)
                .isSystem(false)
                
                // tenant_id is mapped, so Hibernate always sends it: an explicit
                // NULL would override the column DEFAULT instead of triggering it.
                .tenantId(tenantDefaults.current())
                .build());

        auditService.record("folder", folder.getId(), "Created", null, Map.of("path", path));
        return folder;
    }

    @Transactional
    public FileFolder rename(Long id, String name) {
        FileFolder folder = get(id);
        if (folder.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "System folders cannot be renamed");
        }
        String oldPath = folder.getPath();
        String newPath = buildPath(folder.getParent(), name);

        folder.setName(name);
        folder.setPath(newPath);
        repository.save(folder);

        // Descendants carry the old prefix; rewrite them so the tree stays consistent.
        for (FileFolder descendant : repository.findAllByPathStartingWith(oldPath + "/")) {
            descendant.setPath(newPath + descendant.getPath().substring(oldPath.length()));
            repository.save(descendant);
        }

        auditService.record("folder", id, "Updated",
                Map.of("path", oldPath), Map.of("path", newPath));
        return folder;
    }

    @Transactional
    public void delete(Long id) {
        FileFolder folder = get(id);
        if (folder.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System folders cannot be deleted");
        }
        if (repository.existsByParentId(id)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Folder has subfolders and cannot be deleted");
        }
        auditService.record("folder", id, "Deleted", Map.of("path", folder.getPath()), null);
        repository.delete(folder);
    }

    private String buildPath(FileFolder parent, String name) {
        String base = parent == null ? "" : parent.getPath();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + name;
    }
}
