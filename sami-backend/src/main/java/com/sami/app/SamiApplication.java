package com.sami.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Application entry point.
 *
 * <p>JPA auditing is enabled here so {@code @CreatedDate} / {@code @LastModifiedDate}
 * on {@link com.sami.app.common.domain.BaseEntity} are populated automatically.
 */
@SpringBootApplication
@EnableJpaAuditing
public class SamiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SamiApplication.class, args);
    }
}
