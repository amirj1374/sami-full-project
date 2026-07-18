package com.sami.app.supplier;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Supplier configuration, bound from {@code app.suppliers.*}.
 *
 * @param codePrefix  prefix of generated supplier codes (e.g. "SUP-")
 * @param codePadding zero-padded width of the numeric part
 */
@ConfigurationProperties(prefix = "app.suppliers")
public record SupplierProperties(
        String codePrefix,
        int codePadding
) {
}
