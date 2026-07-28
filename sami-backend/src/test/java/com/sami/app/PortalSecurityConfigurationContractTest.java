package com.sami.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards production secret requirements for the customer portal.
 */
class PortalSecurityConfigurationContractTest {

    @Test
    void productionComposeRequiresIndependentPortalJwtSecret() throws IOException {
        String productionCompose = Files.readString(Path.of("docker-compose.prod.yml"));

        assertThat(productionCompose)
                .contains("${PORTAL_JWT_SECRET:?PORTAL_JWT_SECRET is required in production")
                .doesNotContain("PORTAL_JWT_SECRET:-");
    }
}
