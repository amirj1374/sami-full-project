package com.sami.app.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls generation of realistic demo/business data, bound from
 * {@code app.demo.*}.
 *
 * <p>Seeding is idempotent and additive: it runs once on startup only when
 * {@link #enabled()} is true and the business tables are still (near-)empty, so
 * enabling it on an already-populated database is a no-op. The volume is chosen
 * by {@link #size()} — {@code SMALL}, {@code MEDIUM} (default) or {@code LARGE}.
 *
 * @param enabled whether to generate demo data when the database looks empty
 * @param size    volume preset: SMALL | MEDIUM | LARGE
 * @param seed    RNG seed, so a given size always yields the same data set
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(
        boolean enabled,
        String size,
        long seed
) {

    public DemoDataSize resolvedSize() {
        return DemoDataSize.from(size);
    }
}
