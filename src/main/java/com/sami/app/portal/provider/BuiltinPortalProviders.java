package com.sami.app.portal.provider;

import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.portal.repository.PortalDocumentGrantRepository;
import com.sami.app.portal.repository.PortalMessageRepository;
import com.sami.app.portal.repository.PortalNotificationRepository;
import com.sami.app.portal.repository.PortalRequestRepository;
import com.sami.app.portal.spi.PortalContext;
import com.sami.app.portal.spi.PortalDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * The widget providers this module can honestly back today.
 *
 * <p>Every one reads only data the portal itself owns, or the customer's own CRM
 * record. Widgets for installments, repairs, warranty, invoices and trade-ins
 * are catalogued in {@code portal_widgets} but have no provider here on purpose:
 * those modules do not exist yet, and this module must not invent their data.
 * When each is built it contributes its own provider bean and its widget starts
 * working with no change to the portal.
 */
@Configuration
@RequiredArgsConstructor
public class BuiltinPortalProviders {

    private final CustomerRepository customerRepository;
    private final PortalDocumentGrantRepository grantRepository;
    private final PortalRequestRepository requestRepository;
    private final PortalMessageRepository messageRepository;
    private final PortalNotificationRepository notificationRepository;

    @Bean
    public PortalDataProvider portalProfileProvider() {
        return new PortalDataProvider() {
            @Override
            public String key() {
                return "portal.profile";
            }

            @Override
            public String description() {
                return "The customer's own CRM profile summary";
            }

            @Override
            public WidgetData fetch(PortalContext context) {
                return customerRepository.findById(context.customerId())
                        .map(c -> WidgetData.of("My Profile",
                                Map.of("customerId", c.getId(), "language", context.language()),
                                List.of()))
                        .orElseGet(() -> WidgetData.empty("My Profile"));
            }
        };
    }

    @Bean
    public PortalDataProvider portalDocumentsProvider() {
        return new PortalDataProvider() {
            @Override
            public String key() {
                return "portal.documents";
            }

            @Override
            public String description() {
                return "Documents explicitly shared with this customer";
            }

            @Override
            public WidgetData fetch(PortalContext context) {
                var grants = grantRepository
                        .findAllByAccountIdAndRevokedAtIsNullOrderByGrantedAtDesc(context.accountId());
                return WidgetData.of("My Documents",
                        Map.of("total", grants.size(),
                                "awaitingSignature", grants.stream()
                                        .filter(g -> g.isRequiresSignature() && g.getSignedAt() == null)
                                        .count()),
                        grants.stream().limit(10).map(g -> Map.<String, Object>of(
                                "fileUuid", g.getFileUuid().toString(),
                                "title", g.getTitle() == null ? "Document" : g.getTitle(),
                                "grantedAt", g.getGrantedAt().toString(),
                                "requiresSignature", g.isRequiresSignature())).toList());
            }
        };
    }

    @Bean
    public PortalDataProvider portalRequestsProvider() {
        return new PortalDataProvider() {
            @Override
            public String key() {
                return "portal.requests";
            }

            @Override
            public String description() {
                return "The customer's self-service requests";
            }

            @Override
            public WidgetData fetch(PortalContext context) {
                var requests = requestRepository
                        .findAllByAccountIdOrderByCreatedAtDesc(context.accountId());
                return new WidgetData("My Requests",
                        Map.of("total", requests.size(),
                                "open", requestRepository.countOpen(context.accountId())),
                        requests.stream().limit(10).map(r -> Map.<String, Object>of(
                                "requestNumber", r.getRequestNumber(),
                                "subject", r.getSubject(),
                                "type", r.getType().getName(),
                                "status", r.getStatus().getName(),
                                "createdAt", r.getCreatedAt().toString())).toList(),
                        List.of("submit-request"));
            }
        };
    }

    @Bean
    public PortalDataProvider portalMessagesProvider() {
        return new PortalDataProvider() {
            @Override
            public String key() {
                return "portal.messages";
            }

            @Override
            public String description() {
                return "Secure messages between the customer and staff";
            }

            @Override
            public WidgetData fetch(PortalContext context) {
                return WidgetData.of("Messages",
                        Map.of("unread", messageRepository
                                .countByAccountIdAndIsInternalFalseAndReadAtIsNull(context.accountId())),
                        List.of());
            }
        };
    }

    @Bean
    public PortalDataProvider portalNotificationsProvider() {
        return new PortalDataProvider() {
            @Override
            public String key() {
                return "portal.notifications";
            }

            @Override
            public String description() {
                return "In-portal notifications";
            }

            @Override
            public WidgetData fetch(PortalContext context) {
                var unread = notificationRepository
                        .findAllByAccountIdAndReadAtIsNullOrderByCreatedAtDesc(context.accountId());
                return WidgetData.of("Notifications",
                        Map.of("unread", unread.size()),
                        unread.stream().limit(10).map(n -> Map.<String, Object>of(
                                "title", n.getTitle(),
                                "type", n.getType().getName(),
                                "createdAt", n.getCreatedAt().toString())).toList());
            }
        };
    }
}
