package com.sami.app.purchasing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Purchasing configuration, bound from {@code app.purchasing.*}.
 *
 * @param numberIncludeYear whether document numbers carry a year segment
 *                          (PUR-2026-000001 vs PUR-000001)
 * @param numberPadding     zero-padded width of the numeric part
 */
@ConfigurationProperties(prefix = "app.purchasing")
public record PurchasingProperties(
        boolean numberIncludeYear,
        int numberPadding
) {
}
