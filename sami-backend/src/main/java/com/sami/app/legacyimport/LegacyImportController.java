package com.sami.app.legacyimport;

import com.sami.app.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/legacy-imports")
@RequiredArgsConstructor
public class LegacyImportController {
    private final LegacyImportService service;

    @GetMapping @PreAuthorize("@authz.has('legacy-import:view')") public ApiResponse<List<Map<String,Object>>> list(){ return ApiResponse.ok(service.list()); }
    @GetMapping("/{id}") @PreAuthorize("@authz.has('legacy-import:view')") public ApiResponse<Map<String,Object>> detail(@PathVariable Long id){ return ApiResponse.ok(service.batch(id)); }
    @PostMapping(consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('legacy-import:create')") public ApiResponse<Map<String,Object>> upload(@RequestPart("file") MultipartFile file, @RequestParam(required=false) Long migrationGroupId){ return ApiResponse.ok(service.upload(file,migrationGroupId)); }
    @PostMapping("/migration-groups") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('legacy-import:create')") public ApiResponse<Map<String,Object>> createGroup(@RequestBody(required=false) Map<String,String> request){ return ApiResponse.ok(service.createGroup(request==null?null:request.get("name"))); }
    @GetMapping("/migration-groups") @PreAuthorize("@authz.has('legacy-import:view')") public ApiResponse<List<Map<String,Object>>> groups(){ return ApiResponse.ok(service.groups()); }
    @GetMapping("/migration-groups/{groupId}") @PreAuthorize("@authz.has('legacy-import:view')") public ApiResponse<Map<String,Object>> group(@PathVariable Long groupId){ return ApiResponse.ok(service.group(groupId)); }
    @PostMapping("/migration-groups/{groupId}/reconciliation") @PreAuthorize("@authz.has('legacy-import:reconcile')") public ApiResponse<Map<String,Object>> reconcile(@PathVariable Long groupId){ return ApiResponse.ok(service.reconcileGroup(groupId)); }
    @GetMapping("/migration-groups/{groupId}/reconciliation") @PreAuthorize("@authz.has('legacy-import:view')") public ApiResponse<Map<String,Object>> reconciliation(@PathVariable Long groupId){ return ApiResponse.ok(service.reconciliation(groupId)); }
    @PutMapping("/migration-groups/{groupId}/exceptions/{exceptionId}") @PreAuthorize("@authz.has('legacy-import:accept')") public ApiResponse<Map<String,Object>> reviewException(@PathVariable Long groupId,@PathVariable Long exceptionId,@RequestBody Map<String,String> request){ return ApiResponse.ok(service.reviewException(groupId,exceptionId,request.get("approvalStatus"),request.get("explanation"))); }
    @PostMapping("/{id}/analyze") @PreAuthorize("@authz.has('legacy-import:analyze')") public ApiResponse<Map<String,Object>> analyze(@PathVariable Long id){ return ApiResponse.ok(service.analyze(id)); }
    @PostMapping("/{id}/import") @PreAuthorize("@authz.has('legacy-import:execute')") public ApiResponse<Map<String,Object>> execute(@PathVariable Long id){ return ApiResponse.ok(service.execute(id)); }
    @GetMapping("/{id}/files") @PreAuthorize("@authz.has('legacy-import:view')") public ApiResponse<List<Map<String,Object>>> files(@PathVariable Long id){ return ApiResponse.ok(service.files(id)); }
    @GetMapping("/{id}/datasets") @PreAuthorize("@authz.has('legacy-import:view')") public ApiResponse<List<Map<String,Object>>> datasets(@PathVariable Long id){ return ApiResponse.ok(service.datasets(id)); }
    @PutMapping("/{id}/datasets/{datasetId}/report-type") @PreAuthorize("@authz.has('legacy-import:analyze')") public ApiResponse<Map<String,Object>> confirmReportType(@PathVariable Long id,@PathVariable Long datasetId,@RequestBody Map<String,String> request){ return ApiResponse.ok(service.confirmDatasetType(id,datasetId,request.get("reportType"))); }
    @GetMapping("/{id}/messages") @PreAuthorize("@authz.has('legacy-import:view')") public ApiResponse<List<Map<String,Object>>> messages(@PathVariable Long id){ return ApiResponse.ok(service.messages(id)); }
    @GetMapping("/{id}/records") @PreAuthorize("@authz.has('legacy-import:view')") public ApiResponse<List<Map<String,Object>>> records(@PathVariable Long id,@RequestParam(defaultValue="50") int limit,@RequestParam(defaultValue="0") int offset){ return ApiResponse.ok(service.records(id,limit,offset)); }
    @PostMapping("/{id}/comparisons") @PreAuthorize("@authz.has('legacy-import:compare')") public ApiResponse<Map<String,Object>> compare(@PathVariable Long id){ return ApiResponse.ok(service.compare(id)); }
    @PutMapping("/{id}/comparisons/{runId}/records/{recordId}/review") @PreAuthorize("@authz.has('legacy-import:review')") public ApiResponse<Map<String,Object>> review(@PathVariable Long id,@PathVariable Long runId,@PathVariable Long recordId,@RequestBody Map<String,String> request){ return ApiResponse.ok(service.review(id,runId,recordId,request.get("classification"),request.get("note"))); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("@authz.has('legacy-import:delete')") public void delete(@PathVariable Long id){ service.delete(id); }
}
