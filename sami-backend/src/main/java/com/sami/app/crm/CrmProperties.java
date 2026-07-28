package com.sami.app.crm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CRM configuration, bound from {@code app.crm.*}.
 *
 * @param customerCodePrefix  prefix of generated customer codes (e.g. "C")
 * @param customerCodePadding zero-padded width of the numeric part
 */
@ConfigurationProperties(prefix = "app.crm")
public record CrmProperties(
        String customerCodePrefix,
        int customerCodePadding
) {
}
