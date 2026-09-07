package com.sami.app.sales.order;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.sales.order")
public record SalesOrderProperties(Duration correctionWindow) {
    public SalesOrderProperties { correctionWindow = correctionWindow == null ? Duration.ofHours(24) : correctionWindow; }
}
