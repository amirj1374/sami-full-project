package com.sami.app.product.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.product.dto.ProductVariantDtos.*;
import com.sami.app.product.service.ProductVariantService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/products/{productId}/variants") @RequiredArgsConstructor
public class ProductVariantController {
    private final ProductVariantService service;
    @GetMapping @PreAuthorize("@authz.has('products:view')") public ApiResponse<List<Response>> list(@PathVariable Long productId){return ApiResponse.ok(service.list(productId));}
    @PostMapping @PreAuthorize("@authz.has('products:create')") public ApiResponse<Response> create(@PathVariable Long productId,@Valid @RequestBody Request request){return ApiResponse.ok(service.create(productId,request));}
    @GetMapping("/{id}") @PreAuthorize("@authz.has('products:view')") public ApiResponse<Response> get(@PathVariable Long id){return ApiResponse.ok(service.get(id));}
    @PutMapping("/{id}") @PreAuthorize("@authz.has('products:edit')") public ApiResponse<Response> update(@PathVariable Long id,@Valid @RequestBody Request request){return ApiResponse.ok(service.update(id,request));}
}
