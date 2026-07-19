package com.sami.app.portal.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.portal.domain.PortalAccount;
import com.sami.app.portal.domain.PortalAccountCapability;
import com.sami.app.portal.domain.PortalAccountStatus;
import com.sami.app.portal.domain.PortalCapability;
import com.sami.app.portal.event.PortalDomainEvent;
import com.sami.app.portal.repository.PortalAccountCapabilityRepository;
import com.sami.app.portal.repository.PortalAccountRepository;
import com.sami.app.portal.repository.PortalAccountStatusRepository;
import com.sami.app.portal.repository.PortalCapabilityRepository;
import com.sami.app.portal.repository.PortalSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Portal account lifecycle.
 *
 * <p>An account is always bound to an existing CRM customer — the portal never
 * creates customers. That keeps CRM the single source of truth for who a
 * customer is, and means "own data only" resolves through one stable id.
 */
@Service
@RequiredArgsConstructor
public class PortalAccountService {

    private final PortalAccountRepository accountRepository;
    private final PortalAccountStatusRepository statusRepository;
    private final PortalCapabilityRepository capabilityRepository;
    private final PortalAccountCapabilityRepository accountCapabilityRepository;
    private final PortalSessionRepository sessionRepository;
    private final CustomerRepository customerRepository;
    private final PortalAuditService auditService;
    private final TenantDefaults tenantDefaults;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    @Transactional
    public PortalAccount register(Long customerId, String username, String mobile, String email,
                                  String rawPassword, String language, String timezone) {
        // The customer must already exist in CRM.
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                        "No such customer"));

        if (accountRepository.existsByCustomerId(customerId)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "This customer already has a portal account");
        }
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "That username is taken");
        }

        PortalAccountStatus pending = statusRepository.findFirstByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default portal account status is configured"));

        PortalAccount account = accountRepository.save(PortalAccount.builder()
                .customerId(customerId)
                .username(username)
                .mobileNumber(mobile)
                .email(email)
                .passwordHash(rawPassword == null ? null : passwordEncoder.encode(rawPassword))
                .preferredLanguage(language == null || language.isBlank() ? "fa" : language)
                .preferredTimezone(timezone == null || timezone.isBlank() ? "Asia/Tehran" : timezone)
                .status(pending)
                // Explicit for the same reason as elsewhere: a mapped tenant_id
                // is always sent, so the column DEFAULT cannot apply.
                .tenantId(tenantDefaults.current())
                .build());

        grantDefaultCapabilities(account);

        auditService.record(account.getId(), "account", account.getId(),
                PortalAuditService.REGISTERED, null,
                Map.of("customerId", customerId, "username", username));
        events.publishEvent(PortalDomainEvent.of(PortalDomainEvent.CUSTOMER_PORTAL_REGISTERED,
                account.getId(), customerId, Map.of("username", username)));
        return account;
    }

    /** Moves a verified account to the first status that permits sign-in. */
    @Transactional
    public PortalAccount activate(Long accountId) {
        PortalAccount account = require(accountId);
        PortalAccountStatus active = statusRepository.findFirstByAllowsLoginTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No portal account status permits sign-in"));
        account.setStatus(active);
        account.setVerifiedAt(Instant.now());
        accountRepository.save(account);
        auditService.record(accountId, "account", accountId, PortalAuditService.VERIFIED, null, null);
        return account;
    }

    /**
     * Changes status. Any status that forbids sign-in also revokes live sessions,
     * so suspending an account takes effect immediately rather than at expiry.
     */
    @Transactional
    public PortalAccount changeStatus(Long accountId, String statusCode, String reason) {
        PortalAccount account = require(accountId);
        PortalAccountStatus status = statusRepository.findByCode(statusCode)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unknown status: " + statusCode));

        Map<String, Object> before = Map.of("status", account.getStatus().getCode());
        account.setStatus(status);
        if (!status.isAllowsLogin()) {
            sessionRepository.revokeAllForAccount(accountId, Instant.now(),
                    reason == null ? "Account status changed" : reason);
        }
        accountRepository.save(account);
        auditService.record(accountId, "account", accountId, "StatusChanged", before,
                Map.of("status", statusCode, "reason", reason == null ? "" : reason));
        return account;
    }

    @Transactional
    public List<String> setCapabilities(Long accountId, List<String> capabilityCodes) {
        PortalAccount account = require(accountId);
        accountCapabilityRepository.deleteAllByIdAccountId(accountId);
        for (String code : capabilityCodes) {
            PortalCapability capability = capabilityRepository.findByCode(code)
                    .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                            "Unknown capability: " + code));
            accountCapabilityRepository.save(PortalAccountCapability.builder()
                    .id(PortalAccountCapability.Key.builder()
                            .accountId(accountId).capabilityId(capability.getId()).build())
                    .build());
        }
        auditService.record(accountId, "account", accountId,
                PortalAuditService.CAPABILITIES_CHANGED, null, Map.of("capabilities", capabilityCodes));
        return accountCapabilityRepository.findCapabilityCodes(accountId);
    }

    @Transactional
    public void grantDefaultCapabilities(PortalAccount account) {
        for (PortalCapability capability : capabilityRepository.findAllByGrantedByDefaultTrue()) {
            accountCapabilityRepository.save(PortalAccountCapability.builder()
                    .id(PortalAccountCapability.Key.builder()
                            .accountId(account.getId()).capabilityId(capability.getId()).build())
                    .build());
        }
    }

    @Transactional
    public PortalAccount updateProfile(Long accountId, String email, String mobile,
                                       String language, String timezone, Boolean marketingOptIn) {
        PortalAccount account = require(accountId);
        Map<String, Object> before = Map.of(
                "email", account.getEmail() == null ? "" : account.getEmail(),
                "mobile", account.getMobileNumber() == null ? "" : account.getMobileNumber());

        if (email != null) {
            account.setEmail(email);
        }
        if (mobile != null) {
            account.setMobileNumber(mobile);
        }
        if (language != null && !language.isBlank()) {
            account.setPreferredLanguage(language);
        }
        if (timezone != null && !timezone.isBlank()) {
            account.setPreferredTimezone(timezone);
        }
        if (marketingOptIn != null) {
            account.setMarketingOptIn(marketingOptIn);
        }
        accountRepository.save(account);

        auditService.record(accountId, "account", accountId,
                PortalAuditService.PROFILE_UPDATED, before,
                Map.of("email", email == null ? "" : email));
        events.publishEvent(PortalDomainEvent.of(PortalDomainEvent.PROFILE_UPDATED,
                accountId, account.getCustomerId(), Map.of()));
        return account;
    }

    @Transactional(readOnly = true)
    public PortalAccount require(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> ResourceNotFoundException.of("Portal account", accountId));
    }

    @Transactional(readOnly = true)
    public List<String> capabilities(Long accountId) {
        return accountCapabilityRepository.findCapabilityCodes(accountId);
    }
}
