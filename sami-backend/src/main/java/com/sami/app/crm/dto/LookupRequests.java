package com.sami.app.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import com.sami.app.crm.domain.DuplicateRule;

import java.util.List;

/** Request payloads for the CRM's configurable lookup data. */
public final class LookupRequests {

    private static final String SLUG = "^[a-z][a-z0-9-]{1,63}$";
    private static final String SLUG_MESSAGE = "Code must be a lowercase slug (letters, digits, dashes)";

    private LookupRequests() {
    }

    public record TypeRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String code,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            boolean active,
            int displayOrder
    ) {
    }

    public record StatusRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String code,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            boolean isBlocking,
            boolean hiddenByDefault,
            int displayOrder
    ) {
    }

    public record SourceRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String code,
            @NotBlank @Size(max = 100) String name,
            boolean active,
            int displayOrder
    ) {
    }

    public record TagRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 32) String color,
            @Size(max = 255) String description,
            boolean active
    ) {
    }

    /** Full replacement of the duplicate-rule toggles. */
    public record DuplicateRulesRequest(
            @NotEmpty @Valid List<RuleToggle> rules
    ) {
        public record RuleToggle(DuplicateRule.Identifier identifier, boolean enabled) {
        }
    }
}
