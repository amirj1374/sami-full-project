package com.sami.app.knowledge.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.knowledge.domain.KnowledgeArticle;
import com.sami.app.knowledge.repository.ArticleVersionRepository;
import com.sami.app.knowledge.repository.ArticleViewRepository;
import com.sami.app.knowledge.repository.KbApprovalRepository;
import com.sami.app.knowledge.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The module's reports. Returns generic row maps so one payload serves JSON and
 * CSV without a DTO per report, matching the existing report services.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeReportService {

    private static final List<String> REPORTS = List.of(
            "inventory", "most-viewed", "unused", "coverage",
            "pending-reviews", "expired-sops", "revision-history", "usage-statistics");

    private final KnowledgeArticleRepository articleRepository;
    private final ArticleVersionRepository versionRepository;
    private final ArticleViewRepository viewRepository;
    private final KbApprovalRepository approvalRepository;

    public List<String> available() {
        return REPORTS;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> run(String report) {
        return switch (report) {
            case "inventory" -> inventory();
            case "most-viewed" -> mostViewed();
            case "unused" -> unused();
            case "coverage" -> coverage();
            case "pending-reviews" -> pendingReviews();
            case "expired-sops" -> expiredSops();
            case "revision-history" -> revisionHistory();
            case "usage-statistics" -> usageStatistics();
            default -> throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Unknown report '%s'. Available: %s".formatted(report, String.join(", ", REPORTS)));
        };
    }

    private List<Map<String, Object>> inventory() {
        Map<String, List<KnowledgeArticle>> byCategory = articleRepository.findAll().stream()
                .collect(Collectors.groupingBy(a -> a.getCategory().getCode()));
        List<Map<String, Object>> rows = new ArrayList<>();
        byCategory.forEach((category, articles) -> rows.add(row(
                "category", category,
                "total", articles.size(),
                "published", articles.stream().filter(KnowledgeArticle::isPublished).count(),
                "drafts", articles.stream().filter(a -> a.getStatus().isDraftState()).count())));
        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("category"))));
        return rows;
    }

    private List<Map<String, Object>> mostViewed() {
        return articleRepository.findAll().stream()
                .filter(a -> a.getViewCount() > 0)
                .sorted(Comparator.comparingLong(KnowledgeArticle::getViewCount).reversed())
                .limit(50)
                .map(a -> row("code", a.getArticleCode(), "title", a.getTitle(),
                        "views", a.getViewCount(), "lastViewed",
                        a.getLastViewedAt() == null ? null : a.getLastViewedAt().toString()))
                .toList();
    }

    private List<Map<String, Object>> unused() {
        return articleRepository.findUnused().stream()
                .map(a -> row("code", a.getArticleCode(), "title", a.getTitle(),
                        "category", a.getCategory().getCode(),
                        "status", a.getStatus().getCode(),
                        "createdAt", a.getCreatedAt().toString()))
                .toList();
    }

    /**
     * Which business processes readers seek knowledge for, and whether any
     * article is actually bound to them — the gap is where documentation is missing.
     */
    private List<Map<String, Object>> coverage() {
        Map<String, Long> bound = articleRepository.findAll().stream()
                .filter(a -> a.getModuleCode() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getModuleCode() + "/" + (a.getProcessCode() == null ? "*" : a.getProcessCode()),
                        Collectors.counting()));

        List<Map<String, Object>> rows = new ArrayList<>();
        bound.forEach((key, count) -> rows.add(row("process", key, "articles", count)));
        for (Object[] view : viewRepository.countByContext()) {
            String key = view[0] + "/" + (view[1] == null ? "*" : view[1]);
            if (!bound.containsKey(key)) {
                rows.add(row("process", key, "articles", 0L, "note", "viewed but no article bound"));
            }
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("process"))));
        return rows;
    }

    private List<Map<String, Object>> pendingReviews() {
        return articleRepository.findAll().stream()
                .filter(a -> a.getStatus().isReviewState())
                .map(a -> row("code", a.getArticleCode(), "title", a.getTitle(),
                        "pendingStages", approvalRepository
                                .findAllByArticleVersionIdOrderByStageStageOrderAsc(a.getCurrentVersionId())
                                .stream().filter(x -> !x.isDecided()).count()))
                .toList();
    }

    private List<Map<String, Object>> expiredSops() {
        return articleRepository
                .findAllByReviewDateLessThanEqualAndArchivedAtIsNull(LocalDate.now()).stream()
                .map(a -> row("code", a.getArticleCode(), "title", a.getTitle(),
                        "category", a.getCategory().getCode(),
                        "reviewDate", a.getReviewDate().toString(),
                        "overdueDays", java.time.temporal.ChronoUnit.DAYS.between(
                                a.getReviewDate(), LocalDate.now())))
                .toList();
    }

    private List<Map<String, Object>> revisionHistory() {
        return articleRepository.findAll().stream()
                .map(a -> row("code", a.getArticleCode(), "title", a.getTitle(),
                        "versions", versionRepository.countByArticleId(a.getId())))
                .filter(r -> ((Number) r.get("versions")).longValue() > 1)
                .toList();
    }

    private List<Map<String, Object>> usageStatistics() {
        List<KnowledgeArticle> all = articleRepository.findAll();
        long totalViews = all.stream().mapToLong(KnowledgeArticle::getViewCount).sum();
        return List.of(row(
                "articles", all.size(),
                "published", all.stream().filter(KnowledgeArticle::isPublished).count(),
                "totalViews", totalViews,
                "neverViewed", all.stream().filter(a -> a.getViewCount() == 0).count(),
                "reviewOverdue", all.stream().filter(KnowledgeArticle::isReviewOverdue).count()));
    }

    public String toCsv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        StringBuilder csv = new StringBuilder(String.join(",", headers)).append('\n');
        for (Map<String, Object> row : rows) {
            csv.append(headers.stream().map(h -> escape(row.get(h)))
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
                ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }

    private Map<String, Object> row(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
