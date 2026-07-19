package com.sami.app.portal.security;

import com.sami.app.portal.service.PortalPrincipalLoader;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates portal requests.
 *
 * <p><b>Deliberately NOT a {@code @Component}.</b> Spring Boot auto-registers any
 * {@code Filter} bean into the main servlet chain, so annotating it made this
 * filter run on every request in the application — including staff requests,
 * whose tokens it cannot parse by design. It then cleared the security context
 * and wiped the authentication {@code JwtAuthenticationFilter} had just set,
 * which returned 403 for every staff endpoint. It is constructed by
 * {@link PortalSecurityConfig} instead, so it exists only in the portal chain.
 *
 * <p>It is also non-destructive: an authentication established by another realm
 * is left alone. Both properties matter because the realms use different signing
 * keys, so each filter routinely sees tokens it cannot verify.
 *
 * <p>The principal is rebuilt from the database on every request, so a locked
 * account or a revoked session stops working immediately — the "account locked
 * while an active session exists" edge case.
 */
@RequiredArgsConstructor
public class PortalAuthenticationFilter extends OncePerRequestFilter {

    private final PortalTokenService tokenService;
    private final PortalPrincipalLoader principalLoader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Never disturb an authentication established by another realm.
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()
                && !(existing.getPrincipal() instanceof PortalPrincipal)) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = tokenService.parse(header.substring(7));
            Optional<PortalPrincipal> principal = principalLoader.load(
                    tokenService.accountId(claims), tokenService.sessionId(claims));

            principal.ifPresent(p -> SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(p, null, p.authorities())));
        } catch (Exception e) {
            // A token this realm cannot verify — including a staff token presented
            // here. Leave the context as we found it and let authorization reject
            // the request; clearing it is what previously broke staff logins.
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }
}
