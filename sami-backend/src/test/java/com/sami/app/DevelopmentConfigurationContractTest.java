package com.sami.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards development defaults that must stay aligned with the frontend.
 */
class DevelopmentConfigurationContractTest {

    @Test
    void developmentCorsOriginMatchesVitePort() throws IOException {
        assertThat(read("../sami-frontend/vite.config.ts"))
                .contains("port: 7474");
        assertThat(read("src/main/resources/application.yml"))
                .contains("${CORS_ALLOWED_ORIGINS:http://localhost:7474}");
        assertThat(read("docker-compose.yml"))
                .contains("${CORS_ALLOWED_ORIGINS:-http://localhost:7474}");
        assertThat(read(".env.example"))
                .contains("CORS_ALLOWED_ORIGINS=http://localhost:7474");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
