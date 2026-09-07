package com.sami.app.sales.invoice;

import com.sami.app.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.sami.app.sales.invoice.SalesInvoiceDtos.*;

@RestController @RequestMapping("/api/v1/sales-invoices") @RequiredArgsConstructor
public class SalesInvoiceController {
    private final SalesInvoiceService service;
    @GetMapping @PreAuthorize("@authz.has('sales:view')") public ApiResponse<List<Response>> list(@RequestParam Long companyId,@RequestParam Long branchId){return ApiResponse.ok(service.list(companyId,branchId));}
    @GetMapping("/invoiceable/{orderId}") @PreAuthorize("@authz.has('sales:view')") public ApiResponse<List<InvoiceableLine>> invoiceable(@PathVariable Long orderId,@RequestParam Long companyId,@RequestParam Long branchId){return ApiResponse.ok(service.invoiceable(orderId,companyId,branchId));}
    @GetMapping("/{id}") @PreAuthorize("@authz.has('sales:view')") public ApiResponse<Response> get(@PathVariable Long id){return ApiResponse.ok(service.get(id));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('sales:create')") public ApiResponse<Response> create(@Valid @RequestBody Request r){return ApiResponse.ok(service.create(r));}
    @PutMapping("/{id}") @PreAuthorize("@authz.has('sales:edit')") public ApiResponse<Response> update(@PathVariable Long id,@Valid @RequestBody Request r){return ApiResponse.ok(service.update(id,r));}
    @PostMapping("/{id}/issue") @PreAuthorize("@authz.has('sales:confirm')") public ApiResponse<Response> issue(@PathVariable Long id){return ApiResponse.ok(service.issue(id));}
    @PostMapping("/{id}/cancel") @PreAuthorize("@authz.has('sales:edit')") public ApiResponse<Response> cancel(@PathVariable Long id){return ApiResponse.ok(service.cancel(id));}
    @GetMapping("/{id}/audit") @PreAuthorize("@authz.has('sales:view-audit')") public ApiResponse<List<AuditResponse>> audit(@PathVariable Long id){return ApiResponse.ok(service.history(id));}
}
