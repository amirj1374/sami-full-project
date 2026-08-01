package com.sami.app.product.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.product.domain.Product;
import com.sami.app.product.dto.CreateProductRequest;
import com.sami.app.product.dto.ProductFilter;
import com.sami.app.product.dto.ProductResponse;
import com.sami.app.product.dto.UpdateProductRequest;
import com.sami.app.product.repository.ProductRepository;
import com.sami.app.product.repository.ProductSpecifications;
import com.sami.app.product.event.ProductStockChangedEvent;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final TenantContext tenantContext;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(ProductFilter filter, Pageable pageable) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.tenant(tenantContext.requireTenantId()),
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
        Long tenantId = tenantContext.requireTenantId();
        if (productRepository.existsByTenantIdAndSku(tenantId, request.sku())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A product with SKU '%s' already exists".formatted(request.sku()));
        }

        Product product = Product.builder()
                .tenantId(tenantId)
                .name(request.name())
                .sku(request.sku())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .active(request.active())
                .build();

        Product saved = productRepository.saveAndFlush(product);
        if (request.stockQuantity() > 0) {
            publishStockChange(saved, 0, request.stockQuantity());
        }
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = findOrThrow(id);
        int previousStock = product.getStockQuantity();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setActive(request.active());
        if (previousStock != request.stockQuantity()) {
            productRepository.flush();
            publishStockChange(product, previousStock, request.stockQuantity());
        }
        // Dirty checking flushes the update on transaction commit.
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    private void publishStockChange(Product product, int previous, int current) {
        eventPublisher.publishEvent(new ProductStockChangedEvent(
                product.getTenantId(), product.getId(), previous, current,
                CurrentActor.id(), java.time.Instant.now()));
    }
}
