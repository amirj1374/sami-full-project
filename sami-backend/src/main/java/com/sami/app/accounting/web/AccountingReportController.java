package com.sami.app.accounting.web;

import com.sami.app.accounting.service.JournalReportService;
import com.sami.app.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/v1/accounting/reports") @RequiredArgsConstructor
public class AccountingReportController {
    private final JournalReportService reports;
    @GetMapping("/journals") @PreAuthorize("@authz.has('accounting:view')")
    public ApiResponse<List<Map<String,Object>>> journals(@RequestParam(defaultValue="50") int limit) {
        return ApiResponse.ok(reports.entries(limit));
    }
}
