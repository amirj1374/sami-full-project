package com.sami.app.product.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class ProductVariantDtos {
    private ProductVariantDtos() {}
    public record Request(@NotBlank @Size(max=64) String variantCode, @NotBlank @Size(max=255) String name,
                          @Size(max=64) String sku, @NotBlank String status) {}
    public record Response(Long id, Long productId, String variantCode, String name, String sku, String status,
                           Instant createdAt, Instant updatedAt, Long version) {}
}
