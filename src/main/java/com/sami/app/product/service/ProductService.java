package com.sami.app.product.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.product.domain.Product;
import com.sami.app.product.dto.CreateProductRequest;
import com.sami.app.product.dto.ProductFilter;
import com.sami.app.product.dto.ProductResponse;
import com.sami.app.product.dto.UpdateProductRequest;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.product.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product use-cases. Controllers stay thin; all business rules and transaction
 * boundaries live here.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(ProductFilter filter, Pageable pageable) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.nameContains(filter.name()),
                ProductSpecifications.active(filter.active()),
                ProductSpecifications.priceGreaterThanOrEqual(filter.minPrice()),
                ProductSpecifications.priceLessThanOrEqual(filter.maxPrice()));
        return productRepository.findAll(spec, pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findOrThrow(id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A product with SKU '%s' already exists".formatted(request.sku()));
        }

        Product product = Product.builder()
                .name(request.name())
                .sku(request.sku())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .active(request.active())
                .build();

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = findOrThrow(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setActive(request.active());
        // Dirty checking flushes the update on transaction commit.
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }
}
