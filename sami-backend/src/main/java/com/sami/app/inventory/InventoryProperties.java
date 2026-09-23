package com.sami.app.inventory;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Approved Inventory timing configuration. */
@ConfigurationProperties(prefix = "app.inventory")
public record InventoryProperties(Duration reservationTimeout) {
    public Duration reservationTimeoutOrDefault() {
        return reservationTimeout == null || reservationTimeout.isNegative() || reservationTimeout.isZero()
                ? Duration.ofMinutes(30) : reservationTimeout;
    }
}
