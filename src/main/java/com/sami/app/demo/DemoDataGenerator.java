package com.sami.app.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Populates the database with a realistic, interconnected demo dataset on first
 * run when {@code app.demo.enabled=true}. Runs after {@link
 * com.sami.app.config.DataInitializer} so the admin actor exists, and is
 * idempotent: it does nothing once the business tables already hold data.
 *
 * <p>Volume is chosen by {@code app.demo.size} (SMALL | MEDIUM | LARGE). The
 * generator only inserts rows through existing repositories — it never touches
 * the schema — so it is safe to enable on any environment.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class DemoDataGenerator implements CommandLineRunner {

    private final DemoProperties properties;
    private final DemoSeeder seeder;

    @Override
    public void run(String... args) {
        if (!properties.enabled()) {
            return;
        }
        if (seeder.alreadySeeded()) {
            log.info("Demo data generation skipped: business tables already populated.");
            return;
        }

        DemoDataSize size = properties.resolvedSize();
        log.info("Demo data generation starting (size={}, seed={})", size, properties.seed());
        long startedAt = System.currentTimeMillis();

        // Deterministic: a given (size, seed) always yields the same data set.
        int products = seeder.seedProducts(new Random(properties.seed()), size.products());
        int suppliers = seeder.seedSuppliers(new Random(properties.seed() + 1), size.suppliers());
        int customers = seeder.seedCustomers(new Random(properties.seed() + 2), size.customers());
        int purchases = seeder.seedPurchases(new Random(properties.seed() + 3), size.purchases());

        log.info("Demo data generation finished in {} ms — {} products, {} suppliers, {} customers, {} purchases",
                System.currentTimeMillis() - startedAt, products, suppliers, customers, purchases);
    }
}
