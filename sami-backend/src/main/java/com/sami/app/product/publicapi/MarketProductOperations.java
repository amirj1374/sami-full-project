package com.sami.app.product.publicapi;

import java.math.BigDecimal;

public interface MarketProductOperations {
    ProductState upsertSim(String normalizedCode, BigDecimal finalPrice);
    record ProductState(Long productId, boolean created) {}
}
