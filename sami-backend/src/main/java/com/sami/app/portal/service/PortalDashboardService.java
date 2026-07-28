package com.sami.app.portal.service;

import com.sami.app.portal.domain.PortalWidget;
import com.sami.app.portal.repository.PortalWidgetRepository;
import com.sami.app.portal.security.PortalPrincipal;
import com.sami.app.portal.spi.PortalContext;
import com.sami.app.portal.spi.PortalDataProvider;
import com.sami.app.portal.spi.PortalDataProviderRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the customer dashboard from registered providers.
 *
 * <p>A widget whose provider bean is absent is returned as {@code available =
 * false} rather than as an empty panel. That distinction matters to a customer:
 * an empty "Outstanding Installments" panel reads as "you owe nothing", which
 * would be a false statement when the installments module simply is not
 * installed yet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalDashboardService {

    private final PortalWidgetRepository widgetRepository;
    private final PortalDataProviderRegistry providerRegistry;

    public record WidgetResult(String code, String name, boolean available, String unavailableReason,
                               PortalDataProvider.WidgetData data) {
    }

    @Transactional(readOnly = true)
    public List<WidgetResult> dashboard(PortalPrincipal principal) {
        List<WidgetResult> results = new ArrayList<>();

        for (PortalWidget widget : widgetRepository.findAllByEnabledTrueOrderByDisplayOrderAsc()) {
            // A widget the customer lacks the capability for is omitted entirely
            // rather than shown as unavailable — they should not learn it exists.
            if (widget.getRequiredCapability() != null
                    && !principal.can(widget.getRequiredCapability())) {
                continue;
            }

            var provider = providerRegistry.find(widget.getProviderKey());
            if (provider.isEmpty()) {
                results.add(new WidgetResult(widget.getCode(), widget.getName(), false,
                        "No provider is registered for '%s'".formatted(widget.getProviderKey()), null));
                continue;
            }

            PortalContext context = new PortalContext(
                    principal.accountId(), principal.customerId(), principal.tenantId(),
                    principal.language(), principal.timezone(),
                    principal.capabilities(), widget.getConfig());
            try {
                results.add(new WidgetResult(widget.getCode(), widget.getName(), true, null,
                        provider.get().fetch(context)));
            } catch (Exception e) {
                // One failing provider must not take down the whole dashboard.
                log.warn("Portal widget '{}' failed for account {}: {}",
                        widget.getCode(), principal.accountId(), e.toString());
                results.add(new WidgetResult(widget.getCode(), widget.getName(), false,
                        "This section is temporarily unavailable", null));
            }
        }
        return results;
    }
}
