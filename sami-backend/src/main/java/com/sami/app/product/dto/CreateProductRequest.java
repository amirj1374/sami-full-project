package com.sami.app.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Payload to create a product. */
public record CreateProductRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "SKU is required")
        @Size(max = 64)
        String sku,

        @Size(max = 2000)
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price must be zero or positive")
        @Digits(integer = 10, fraction = 2, message = "Price must have at most 2 decimals")
        BigDecimal price,

        @PositiveOrZero(message = "Stock quantity must be zero or positive")
        int stockQuantity,

        boolean active,

        boolean hamtaEligible
) {
}
