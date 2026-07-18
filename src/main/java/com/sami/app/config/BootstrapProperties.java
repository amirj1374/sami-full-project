package com.sami.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls first-run seeding of a default admin account, bound from
 * {@code app.bootstrap.admin.*}.
 *
 * @param enabled  whether to create the admin if it is missing
 * @param email    admin login
 * @param password admin password (BCrypt-hashed at seed time)
 * @param fullName admin display name
 */
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record BootstrapProperties(
        boolean enabled,
        String email,
        String password,
        String fullName
) {
}
