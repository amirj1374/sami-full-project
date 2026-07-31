package com.sami.app.files.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.files.domain.ManagedFile;
import com.sami.app.files.repository.FileVersionRepository;
import com.sami.app.files.repository.ManagedFileRepository;
import com.sami.app.files.repository.StorageProviderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The module's reports. Returns generic row maps so the same payload serves JSON
 * and CSV without a per-report DTO, matching {@code LicenseReportService}.
 */
@Service
@RequiredArgsConstructor
public class FileReportService {

    private static final List<String> REPORTS = List.of(
            "storage-usage", "largest-files", "file-growth", "storage-by-module",
            "storage-by-company", "version-history", "retention", "media-statistics",
            "provider-usage");

    private final ManagedFileRepository fileRepository;
    private final FileVersionRepository versionRepository;
    private final StorageProviderConfigRepository providerRepository;

    public List<String> available() {
        return REPORTS;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> run(String report) {
        return switch (report) {
            case "storage-usage" -> storageUsage();
            case "largest-files" -> largestFiles();
            case "file-growth" -> fileGrowth();
            case "storage-by-module" -> groupBy(ManagedFile::getModuleCode, "module");
            case "storage-by-company" -> groupBy(f -> String.valueOf(f.getCompanyId()), "company");
            case "version-history" -> versionHistory();
            case "retention" -> retention();
            case "media-statistics" -> mediaStatistics();
            case "provider-usage" -> providerUsage();
            default -> throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Unknown report '%s'. Available: %s".formatted(report, String.join(", ", REPORTS)));
        };
    }

    private List<Map<String, Object>> storageUsage() {
        return List.of(row(
                "totalFiles", fileRepository.countByDeletedAtIsNull(),
                "totalBytes", fileRepository.totalBytes(),
                "totalMegabytes", fileRepository.totalBytes() / (1024 * 1024)));
    }

    private List<Map<String, Object>> largestFiles() {
        return fileRepository.findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "sizeBytes")))
                .stream()
                .filter(f -> !f.isDeleted())
                .map(f -> row("fileCode", f.getFileCode(), "name", f.getName(),
                        "sizeBytes", f.getSizeBytes(), "category", f.getCategory().getCode()))
                .toList();
    }

    private List<Map<String, Object>> fileGrowth() {
        Map<String, List<ManagedFile>> byMonth = fileRepository.findAll().stream()
                .filter(f -> !f.isDeleted())
                .collect(Collectors.groupingBy(f -> f.getCreatedAt().toString().substring(0, 7),
                        LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> rows = new ArrayList<>();
        byMonth.forEach((month, files) -> rows.add(row(
                "period", month,
                "files", files.size(),
                "bytes", files.stream().mapToLong(ManagedFile::getSizeBytes).sum())));
        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("period"))));
        return rows;
    }

    private List<Map<String, Object>> groupBy(java.util.function.Function<ManagedFile, String> key,
                                              String label) {
        Map<String, List<ManagedFile>> grouped = fileRepository.findAll().stream()
                .filter(f -> !f.isDeleted())
                .filter(f -> key.apply(f) != null)
                .collect(Collectors.groupingBy(key));
        List<Map<String, Object>> rows = new ArrayList<>();
        grouped.forEach((k, files) -> rows.add(row(
                label, k,
                "files", files.size(),
                "bytes", files.stream().mapToLong(ManagedFile::getSizeBytes).sum())));
        rows.sort(Comparator.comparingLong(r -> -((Number) r.get("bytes")).longValue()));
        return rows;
    }

    private List<Map<String, Object>> versionHistory() {
        return fileRepository.findAll().stream()
                .filter(f -> !f.isDeleted())
                .map(f -> row("fileCode", f.getFileCode(), "name", f.getName(),
                        "versions", versionRepository.countByFileId(f.getId())))
                .filter(r -> ((Number) r.get("versions")).longValue() > 1)
                .toList();
    }

    private List<Map<String, Object>> retention() {
        return fileRepository.findAll().stream()
                .filter(f -> f.getRetentionExpiresAt() != null && !f.isDeleted())
                .map(f -> row("fileCode", f.getFileCode(),
                        "policy", f.getRetentionPolicy() == null ? null : f.getRetentionPolicy().getCode(),
                        "expiresAt", f.getRetentionExpiresAt().toString(),
                        "legalHold", f.isUnderLegalHold()))
                .toList();
    }

    private List<Map<String, Object>> mediaStatistics() {
        Map<String, List<ManagedFile>> byCategory = fileRepository.findAll().stream()
                .filter(f -> !f.isDeleted())
                .collect(Collectors.groupingBy(f -> f.getCategory().getCode()));
        List<Map<String, Object>> rows = new ArrayList<>();
        byCategory.forEach((code, files) -> rows.add(row(
                "category", code,
                "files", files.size(),
                "bytes", files.stream().mapToLong(ManagedFile::getSizeBytes).sum(),
                "averageBytes", files.stream().mapToLong(ManagedFile::getSizeBytes).sum() / files.size())));
        return rows;
    }

    private List<Map<String, Object>> providerUsage() {
        return providerRepository.findAllByOrderByPriorityAsc().stream()
                .map(p -> row("provider", p.getCode(), "handler", p.getHandlerKey(),
                        "enabled", p.isEnabled(), "isDefault", p.isDefault()))
                .toList();
    }

    /** CSV rendering for the export endpoint. */
    public String toCsv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "\uFEFF";
        }
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        StringBuilder csv = new StringBuilder("\uFEFF").append(String.join(",", headers)).append('\n');
        for (Map<String, Object> row : rows) {
            csv.append(headers.stream()
                    .map(h -> escape(row.get(h)))
                    .collect(Collectors.joining(","))).append('\n');
        }
        return csv.toString();
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return text.contains(",") || text.contains("\"") || text.contains("\n")
                ? '"' + text.replace("\"", "\"\"") + '"'
                : text;
    }

    private Map<String, Object> row(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
