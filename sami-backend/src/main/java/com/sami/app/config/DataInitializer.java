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
import org.springframework.jdbc.core.JdbcTemplate;

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
    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        if (!properties.enabled()) {
            return;
        }
        User admin = userRepository.findByEmail(properties.email()).orElseGet(() -> createAdmin());
        provisionInitialOrganizationScope(admin);
    }

    private User createAdmin() {
        // Resolved by flag, not by name: whichever role carries is_super_admin is
        // the one the bootstrap admin gets. Its absence means the RBAC seed
        // migrations did not run, which is fatal.
        Role adminRole = roleRepository.findFirstByIsSuperAdminTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No super-admin role found; RBAC seed migrations must run before bootstrap"));
        UserStatus defaultStatus = statusRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No default user status found; user-management seed migrations must run first"));

        User admin = userRepository.save(User.builder()
                .email(properties.email())
                .passwordHash(passwordEncoder.encode(properties.password()))
                .fullName(properties.fullName())
                .role(adminRole)
                .status(defaultStatus)
                .build());
        profileRepository.save(UserProfile.builder()
                .user(admin)
                .displayName(properties.fullName())
                .build());
        log.info("Seeded default admin account: {}", properties.email());
        return admin;
    }

    /**
     * Flyway runs before this initializer, so a first-run administrator is not
     * present for V51's legacy-user bootstrap.  Provision only the configured
     * bootstrap identity's initial default scope, without reviving a grant that
     * an administrator later revoked.
     */
    private void provisionInitialOrganizationScope(User admin) {
        Long tenantId = jdbc.queryForObject("select tenant_id from users where id=?", Long.class, admin.getId());
        Long companyId = jdbc.query("select id from companies where tenant_id=? and is_default and is_active", rs -> rs.next() ? rs.getLong(1) : null, tenantId);
        if (companyId == null) {
            log.warn("Bootstrap administrator has no default Company scope; assign it through organization administration.");
            return;
        }
        Long assignmentId = jdbc.query("insert into user_company_roles(tenant_id,user_id,company_id,role_id) values(?,?,?,?) on conflict(tenant_id,user_id,company_id) do nothing returning id",
                rs -> rs.next() ? rs.getLong(1) : null, tenantId, admin.getId(), companyId, admin.getRole().getId());
        if (assignmentId == null) {
            return;
        }
        jdbc.update("insert into foundation_audit_logs(tenant_id,company_id,subject_type,subject_id,action,reason,context) values(?,?,?,?,'BOOTSTRAP_ASSIGNED','BOOTSTRAP_ADMIN_INITIAL_SCOPE','{}'::jsonb)",
                tenantId, companyId, "USER_COMPANY_ROLE", assignmentId);
        Long branchId = jdbc.query("select id from branches where tenant_id=? and company_id=? and is_default and is_active", rs -> rs.next() ? rs.getLong(1) : null, tenantId, companyId);
        if (branchId == null) {
            log.warn("Bootstrap administrator has no default Branch scope; assign it through organization administration.");
            return;
        }
        Long grantId = jdbc.query("insert into user_branch_grants(tenant_id,assignment_id,branch_id) values(?,?,?) on conflict(assignment_id,branch_id) do nothing returning id",
                rs -> rs.next() ? rs.getLong(1) : null, tenantId, assignmentId, branchId);
        if (grantId != null) {
            jdbc.update("insert into foundation_audit_logs(tenant_id,company_id,branch_id,subject_type,subject_id,action,reason,context) values(?,?,?,?,?,'BOOTSTRAP_GRANTED','BOOTSTRAP_ADMIN_INITIAL_SCOPE','{}'::jsonb)",
                    tenantId, companyId, branchId, "USER_BRANCH_GRANT", grantId);
        }
    }
}
