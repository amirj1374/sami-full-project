package com.sami.app.security;

import com.sami.app.authz.domain.Permission;
import com.sami.app.user.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Adapts our domain {@link User} to Spring Security's {@link UserDetails}.
 *
 * <p>Authorities are the role's permission codes ({@code module:action}) with no
 * {@code ROLE_} prefix — authorization is permission-based and evaluated through
 * the {@code @authz} bean, never via {@code hasRole(...)}. A super-admin role has
 * no permission rows; {@link #isSuperAdmin()} exposes the bypass flag instead.
 */
public record SecurityUser(User user) implements UserDetails {

    public Long getId() {
        return user.getId();
    }

    /** @return true if the user's role bypasses all permission checks. */
    public boolean isSuperAdmin() {
        return user.getRole().isSuperAdmin();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRole().getPermissions().stream()
                .map(Permission::getCode)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Whether login is permitted is a property of the user's configurable status. */
    @Override
    public boolean isEnabled() {
        return user.isLoginAllowed();
    }
}
