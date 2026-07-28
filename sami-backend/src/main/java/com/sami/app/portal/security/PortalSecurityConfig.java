package com.sami.app.portal.security;

import com.sami.app.portal.service.PortalPrincipalLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * A second, independent security filter chain for {@code /api/v1/portal/**}.
 *
 * <p>Ordered ahead of the staff chain and scoped by {@code securityMatcher}, so
 * the existing {@code SecurityConfig} is untouched: it keeps handling every
 * other path exactly as before.
 *
 * <p>Only registration, login and OTP verification are public. Everything else
 * requires an authenticated portal principal — and because these paths are the
 * first surface exposed to the open internet, they are the reason account
 * lockout lives on {@code portal_accounts}.
 */
@Configuration
@RequiredArgsConstructor
public class PortalSecurityConfig {

    private static final String[] PUBLIC_PORTAL_ENDPOINTS = {
            "/api/v1/portal/auth/register",
            "/api/v1/portal/auth/login",
            "/api/v1/portal/auth/otp/**",
            "/api/v1/portal/auth/verify"
    };

    private final PortalTokenService portalTokenService;
    private final PortalPrincipalLoader portalPrincipalLoader;

    /**
     * Built here rather than declared a {@code @Component}: a {@code Filter} bean
     * is auto-registered by Spring Boot into the main servlet chain, which would
     * run it against every staff request. Constructing it locally confines it to
     * the portal chain below.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain portalFilterChain(HttpSecurity http) throws Exception {
        PortalAuthenticationFilter portalAuthenticationFilter =
                new PortalAuthenticationFilter(portalTokenService, portalPrincipalLoader);
        http
                .securityMatcher("/api/v1/portal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PORTAL_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(portalAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
