package com.sami.app.sales.delivery;

import com.sami.app.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.sami.app.sales.delivery.DeliveryDtos.*;

@RestController @RequestMapping("/api/v1/sales-deliveries") @RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService service;
    @GetMapping @PreAuthorize("@authz.has('sales:view')") public ApiResponse<List<Response>> list(@RequestParam Long companyId,@RequestParam Long branchId){return ApiResponse.ok(service.list(companyId,branchId));}
    @GetMapping("/{id}") @PreAuthorize("@authz.has('sales:view')") public ApiResponse<Response> get(@PathVariable Long id){return ApiResponse.ok(service.get(id));}
    @PostMapping("/from-order/{orderId}") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('sales:create')") public ApiResponse<Response> create(@PathVariable Long orderId,@Valid @RequestBody Request r){return ApiResponse.ok(service.create(orderId,r));}
    @PutMapping("/{id}") @PreAuthorize("@authz.has('sales:edit')") public ApiResponse<Response> update(@PathVariable Long id,@Valid @RequestBody Request r){return ApiResponse.ok(service.update(id,r));}
    @PostMapping("/{id}/confirm") @PreAuthorize("@authz.has('sales:confirm')") public ApiResponse<Response> confirm(@PathVariable Long id){return ApiResponse.ok(service.confirm(id));}
    @PostMapping("/{id}/cancel") @PreAuthorize("@authz.has('sales:edit')") public ApiResponse<Response> cancel(@PathVariable Long id){return ApiResponse.ok(service.cancel(id));}
    @GetMapping("/{id}/audit") @PreAuthorize("@authz.has('sales:view-audit')") public ApiResponse<List<AuditResponse>> audit(@PathVariable Long id){return ApiResponse.ok(service.history(id));}
}
