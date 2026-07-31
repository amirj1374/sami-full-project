package com.sami.app.sales.web;

import com.sami.app.common.api.*; import com.sami.app.sales.dto.SalesDtos.*; import com.sami.app.sales.service.SalesService;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.tags.Tag; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable; import org.springframework.data.web.PageableDefault; import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/sales") @RequiredArgsConstructor
@Tag(name="Sales",description="Tenant-scoped sales, payments, lifecycle, returns and reporting")
public class SalesController {
 private final SalesService service;
 @GetMapping @PreAuthorize("@authz.has('sales:view')") @Operation(summary="List sales")
 public ApiResponse<PageResponse<SaleResponse>> list(@PageableDefault(size=20,sort="createdAt") Pageable pageable){return ApiResponse.ok(PageResponse.from(service.list(pageable)));}
 @GetMapping("/{id}") @PreAuthorize("@authz.has('sales:view')") public ApiResponse<SaleResponse> get(@PathVariable Long id){return ApiResponse.ok(service.get(id));}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('sales:create')") public ApiResponse<SaleResponse> create(@Valid @RequestBody SaleRequest request){return ApiResponse.ok(service.create(request));}
 @PutMapping("/{id}") @PreAuthorize("@authz.has('sales:edit')") public ApiResponse<SaleResponse> update(@PathVariable Long id,@Valid @RequestBody SaleRequest request){return ApiResponse.ok(service.update(id,request));}
 @PostMapping("/{id}/discounts") @PreAuthorize("@authz.has('sales:edit')") public ApiResponse<SaleResponse> discount(@PathVariable Long id,@Valid @RequestBody DiscountRequest request){return ApiResponse.ok(service.requestDiscount(id,request));}
 @GetMapping("/{id}/discounts") @PreAuthorize("@authz.has('sales:view')") public ApiResponse<List<DiscountResponse>> discounts(@PathVariable Long id){return ApiResponse.ok(service.discountHistory(id));}
 @PostMapping("/{id}/discounts/{discountId}/decision") @PreAuthorize("@authz.has('sales:approve-discount')") public ApiResponse<SaleResponse> decide(@PathVariable Long id,@PathVariable Long discountId,@RequestBody DecisionRequest request){return ApiResponse.ok(service.decideDiscount(id,discountId,request));}
 @PostMapping("/{id}/payments") @PreAuthorize("@authz.has('sales:payment')") public ApiResponse<SaleResponse> payment(@PathVariable Long id,@Valid @RequestBody PaymentRequest request){return ApiResponse.ok(service.addPayment(id,request));}
 @PostMapping("/{id}/confirm") @PreAuthorize("@authz.has('sales:confirm')") public ApiResponse<SaleResponse> confirm(@PathVariable Long id){return ApiResponse.ok(service.confirm(id));}
 @PostMapping("/{id}/complete") @PreAuthorize("@authz.has('sales:complete')") public ApiResponse<SaleResponse> complete(@PathVariable Long id){return ApiResponse.ok(service.complete(id));}
 @PostMapping("/{id}/cancel") @PreAuthorize("@authz.has('sales:cancel')") public ApiResponse<SaleResponse> cancel(@PathVariable Long id,@Valid @RequestBody CancelRequest request){return ApiResponse.ok(service.cancel(id,request));}
 @PostMapping("/{id}/return") @PreAuthorize("@authz.has('sales:return')") public ApiResponse<SaleResponse> createReturn(@PathVariable Long id,@Valid @RequestBody ReturnRequest request){return ApiResponse.ok(service.createReturn(id,request));}
 @GetMapping("/{id}/audit") @PreAuthorize("@authz.has('sales:view-audit')") public ApiResponse<List<AuditResponse>> audit(@PathVariable Long id){return ApiResponse.ok(service.audit(id));}
 @GetMapping("/dashboard") @PreAuthorize("@authz.has('sales:report')") public ApiResponse<DashboardResponse> dashboard(){return ApiResponse.ok(service.dashboard());}
 @GetMapping(value="/reports/export.csv",produces="text/csv;charset=UTF-8") @PreAuthorize("@authz.has('sales:export')") public ResponseEntity<byte[]> export(){return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''sales-report.csv").body(service.exportCsv());}
 @PostMapping("/ai/recommendation") @PreAuthorize("@authz.has('sales:view')") public ApiResponse<AiResponse> recommend(@Valid @RequestBody AiRequest request){return ApiResponse.ok(service.recommend(request));}
}
