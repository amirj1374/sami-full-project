package com.sami.app.crm.web;
import com.sami.app.common.api.ApiResponse;
import com.sami.app.crm.service.CrmIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/crm/intelligence") @RequiredArgsConstructor
public class CrmIntelligenceController {
 private final CrmIntelligenceService service;
 @GetMapping("/customers/{customerId}") @PreAuthorize("@authz.has('customers:view')")
 public ApiResponse<CrmIntelligenceService.Intelligence> customer(@PathVariable Long customerId){return ApiResponse.ok(service.evaluate(customerId));}
}
