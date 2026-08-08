package com.sami.app.marketsync;

import com.sami.app.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/v1/market-sync") @RequiredArgsConstructor
public class MarketSyncController {
    private final MarketSyncService service;
    @GetMapping("/overview") @PreAuthorize("@authz.has('market-sync:view')") public ApiResponse<Map<String,Object>> overview(){return ApiResponse.ok(service.overview());}
    @GetMapping("/sources") @PreAuthorize("@authz.has('market-sync:view')") public ApiResponse<List<Map<String,Object>>> sources(){return ApiResponse.ok(service.sources());}
    @PostMapping("/sources") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('market-sync:manage-sources')") public ApiResponse<Map<String,Object>> createSource(@RequestBody Map<String,Object> r){return ApiResponse.ok(service.saveSource(null,r));}
    @PutMapping("/sources/{id}") @PreAuthorize("@authz.has('market-sync:manage-sources')") public ApiResponse<Map<String,Object>> updateSource(@PathVariable Long id,@RequestBody Map<String,Object> r){return ApiResponse.ok(service.saveSource(id,r));}
    @GetMapping("/pricing-profiles") @PreAuthorize("@authz.has('market-sync:view')") public ApiResponse<List<Map<String,Object>>> profiles(){return ApiResponse.ok(service.profiles());}
    @PostMapping("/pricing-profiles") @PreAuthorize("@authz.has('market-sync:manage-pricing')") public ApiResponse<Map<String,Object>> createProfile(@RequestBody Map<String,Object> r){return ApiResponse.ok(service.saveProfile(null,r));}
    @PutMapping("/pricing-profiles/{id}") @PreAuthorize("@authz.has('market-sync:manage-pricing')") public ApiResponse<Map<String,Object>> updateProfile(@PathVariable Long id,@RequestBody Map<String,Object> r){return ApiResponse.ok(service.saveProfile(id,r));}
    @PostMapping("/pricing-preview") @PreAuthorize("@authz.has('market-sync:manage-pricing')") public ApiResponse<Map<String,Object>> preview(@RequestBody Map<String,Object> r){return ApiResponse.ok(service.preview(r));}
    @GetMapping("/publication-rules") @PreAuthorize("@authz.has('market-sync:view')") public ApiResponse<List<Map<String,Object>>> rules(){return ApiResponse.ok(service.rules());}
    @PostMapping("/publication-rules") @PreAuthorize("@authz.has('market-sync:manage-publication')") public ApiResponse<Map<String,Object>> createRule(@RequestBody Map<String,Object> r){return ApiResponse.ok(service.saveRule(r));}
    @DeleteMapping("/publication-rules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("@authz.has('market-sync:manage-publication')") public void deleteRule(@PathVariable Long id){service.deleteRule(id);}
    @PostMapping("/sources/{id}/sync") @PreAuthorize("@authz.has('market-sync:execute')") public ApiResponse<Map<String,Object>> sync(@PathVariable Long id){return ApiResponse.ok(service.sync(id,"MANUAL"));}
    @GetMapping("/runs") @PreAuthorize("@authz.has('market-sync:view-history')") public ApiResponse<List<Map<String,Object>>> runs(){return ApiResponse.ok(service.runs());}
    @GetMapping("/price-history") @PreAuthorize("@authz.has('market-sync:view-history')") public ApiResponse<List<Map<String,Object>>> history(){return ApiResponse.ok(service.history());}
    @GetMapping("/errors") @PreAuthorize("@authz.has('market-sync:view-errors')") public ApiResponse<List<Map<String,Object>>> errors(){return ApiResponse.ok(service.errors());}
    @GetMapping("/products/{productId}") @PreAuthorize("@authz.has('market-sync:view')") public ApiResponse<Map<String,Object>> product(@PathVariable Long productId){return ApiResponse.ok(service.productStatus(productId));}
    @PostMapping("/products/{productId}/resolve-source/{sourceId}") @PreAuthorize("@authz.has('market-sync:manage-sources')") public ApiResponse<Map<String,Object>> resolve(@PathVariable Long productId,@PathVariable Long sourceId){return ApiResponse.ok(service.resolveConflict(productId,sourceId));}
    @PostMapping("/products/{productId}/reactivate") @PreAuthorize("@authz.has('market-sync:manage-publication')") public ApiResponse<Map<String,Object>> reactivate(@PathVariable Long productId){return ApiResponse.ok(service.reactivate(productId));}
}
