package com.sami.app.product.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.product.dto.CreateProductRequest;
import com.sami.app.product.dto.ProductFilter;
import com.sami.app.product.dto.ProductResponse;
import com.sami.app.product.dto.UpdateProductRequest;
import com.sami.app.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product CRUD.
 *
 * <p>Every endpoint declares its required permission code via the {@code @authz}
 * bean — reads need {@code products:view}. Listing supports pagination, sorting
 * ({@code ?page=0&size=20&sort=price,desc}) and filtering (query params bound to
 * {@link ProductFilter}).
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalogue")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("@authz.has('products:view')")
    @Operation(summary = "List products (paginated, filterable, sortable)")
    public ApiResponse<PageResponse<ProductResponse>> list(
            ProductFilter filter,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(productService.list(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('products:view')")
    @Operation(summary = "Get a product by id")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('products:create')")
    @Operation(summary = "Create a product")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('products:edit')")
    @Operation(summary = "Update a product")
    public ApiResponse<ProductResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('products:delete')")
    @Operation(summary = "Delete a product")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
