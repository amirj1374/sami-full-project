package com.sami.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS configuration, bound from {@code app.cors.*}. Origins are environment-specific
 * (localhost in dev, the real domain in prod) and supplied via configuration.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
