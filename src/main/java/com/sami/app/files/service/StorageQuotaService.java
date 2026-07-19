package com.sami.app.files.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.files.FilesProperties;
import com.sami.app.files.domain.ManagedFile;
import com.sami.app.files.domain.StorageQuota;
import com.sami.app.files.repository.ManagedFileRepository;
import com.sami.app.files.repository.StorageQuotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Enforces configurable storage ceilings by tenant, company, branch, module or
 * provider. Evaluated before bytes are accepted.
 */
@Service
@RequiredArgsConstructor
public class StorageQuotaService {

    private final StorageQuotaRepository quotaRepository;
    private final ManagedFileRepository fileRepository;
    private final FilesProperties properties;

    /** @return the quotas the incoming file would breach; empty when it fits. */
    @Transactional(readOnly = true)
    public List<Breach> check(ManagedFile candidate, long incomingBytes) {
        if (!properties.quotasEnforced()) {
            return List.of();
        }
        List<Breach> breaches = new ArrayList<>();
        for (StorageQuota quota : quotaRepository.findAllByEnabledTrue()) {
            if (!appliesTo(quota, candidate)) {
                continue;
            }
            long used = usedBytes(quota, candidate);
            if (quota.getMaxBytes() != null && used + incomingBytes > quota.getMaxBytes()) {
                breaches.add(new Breach(quota.getScopeKind(), quota.getScopeRef(),
                        quota.getMaxBytes(), used, incomingBytes));
            }
        }
        return breaches;
    }

    @Transactional(readOnly = true)
    public void enforce(ManagedFile candidate, long incomingBytes) {
        List<Breach> breaches = check(candidate, incomingBytes);
        if (!breaches.isEmpty()) {
            Breach first = breaches.get(0);
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Storage quota exceeded for %s%s: %d MB used of %d MB"
                            .formatted(first.scopeKind(),
                                    first.scopeRef() == null ? "" : " " + first.scopeRef(),
                                    first.usedBytes() / (1024 * 1024),
                                    first.maxBytes() / (1024 * 1024)));
        }
    }

    private boolean appliesTo(StorageQuota quota, ManagedFile file) {
        return switch (quota.getScopeKind()) {
            case "TENANT" -> true;
            case "COMPANY" -> file.getCompanyId() != null
                    && Objects.equals(String.valueOf(file.getCompanyId()), quota.getScopeRef());
            case "BRANCH" -> file.getBranchId() != null
                    && Objects.equals(String.valueOf(file.getBranchId()), quota.getScopeRef());
            case "MODULE" -> Objects.equals(file.getModuleCode(), quota.getScopeRef());
            case "PROVIDER" -> true;
            default -> false;
        };
    }

    private long usedBytes(StorageQuota quota, ManagedFile file) {
        return switch (quota.getScopeKind()) {
            case "COMPANY" -> fileRepository.totalBytesByCompany(file.getCompanyId());
            case "BRANCH" -> fileRepository.totalBytesByBranch(file.getBranchId());
            case "MODULE" -> fileRepository.totalBytesByModule(file.getModuleCode());
            default -> fileRepository.totalBytes();
        };
    }

    public record Breach(String scopeKind, String scopeRef, long maxBytes,
                         long usedBytes, long incomingBytes) {
    }
}
