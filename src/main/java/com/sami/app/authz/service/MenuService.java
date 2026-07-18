package com.sami.app.authz.service;

import com.sami.app.authz.dto.MenuItemResponse;
import com.sami.app.authz.repository.AppModuleRepository;
import com.sami.app.security.Authz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Builds the current user's navigation menu: enabled modules in display order,
 * narrowed to those whose {@code <code>:view} permission the user holds. The
 * super-admin bypass inside {@link Authz#has} means super admins see every
 * enabled module without special-casing here.
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final AppModuleRepository moduleRepository;
    private final Authz authz;

    @Transactional(readOnly = true)
    public List<MenuItemResponse> menuForCurrentUser() {
        return moduleRepository.findByEnabledTrueOrderByDisplayOrderAsc().stream()
                .filter(module -> authz.has(module.getCode() + ":view"))
                .map(MenuItemResponse::from)
                .toList();
    }
}
