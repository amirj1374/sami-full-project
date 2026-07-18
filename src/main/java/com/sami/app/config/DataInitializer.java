package com.sami.app.config;

import com.sami.app.authz.domain.Role;
import com.sami.app.authz.repository.RoleRepository;
import com.sami.app.user.domain.User;
import com.sami.app.user.domain.UserProfile;
import com.sami.app.user.domain.UserStatus;
import com.sami.app.user.repository.UserProfileRepository;
import com.sami.app.user.repository.UserRepository;
import com.sami.app.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default administrator on first run so the system is immediately usable.
 *
 * <p>We do this in code rather than in a Flyway migration because the password must
 * be BCrypt-hashed by the same encoder the app uses; hardcoding a hash in SQL is
 * brittle and leaks credentials into version control. The seed is idempotent (skips
 * if the admin already exists) and can be disabled via {@code app.bootstrap.admin.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BootstrapProperties properties;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserStatusRepository statusRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!properties.enabled()) {
            return;
        }
        if (userRepository.existsByEmail(properties.email())) {
            log.debug("Bootstrap admin '{}' already exists; skipping seed.", properties.email());
            return;
        }

        // Resolved by flag, not by name: whichever role carries is_super_admin is
        // the one the bootstrap admin gets. Its absence means the RBAC seed
        // migrations did not run, which is fatal.
        Role adminRole = roleRepository.findFirstByIsSuperAdminTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No super-admin role found; RBAC seed migrations must run before bootstrap"));

        UserStatus defaultStatus = statusRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No default user status found; user-management seed migrations must run first"));

        User admin = User.builder()
                .email(properties.email())
                .passwordHash(passwordEncoder.encode(properties.password()))
                .fullName(properties.fullName())
                .role(adminRole)
                .status(defaultStatus)
                .build();
        userRepository.save(admin);

        profileRepository.save(UserProfile.builder()
                .user(admin)
                .displayName(properties.fullName())
                .build());
        log.info("Seeded default admin account: {}", properties.email());
    }
}
