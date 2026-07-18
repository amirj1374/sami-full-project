package com.sami.app.supplier.dto;

import com.sami.app.supplier.domain.SupCategory;
import com.sami.app.supplier.domain.SupDocumentType;
import com.sami.app.supplier.domain.SupPaymentTerm;
import com.sami.app.supplier.domain.SupRatingCriterion;
import com.sami.app.supplier.domain.SupStatus;
import com.sami.app.supplier.domain.SupTag;
import com.sami.app.supplier.domain.SupType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Responses/requests for the supplier module's configurable lookups. */
public final class SupLookupDtos {

    private static final String SLUG = "^[a-z][a-z0-9-]{1,63}$";
    private static final String SLUG_MESSAGE = "Code must be a lowercase slug (letters, digits, dashes)";

    private SupLookupDtos() {
    }

    public record TypeResponse(Long id, String code, String name, String description,
                               boolean isDefault, boolean active, boolean isSystem, int displayOrder) {
        public static TypeResponse from(SupType t) {
            return new TypeResponse(t.getId(), t.getCode(), t.getName(), t.getDescription(),
                    t.isDefault(), t.isActive(), t.isSystem(), t.getDisplayOrder());
        }
    }

    public record TypeRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String code,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            boolean active,
            int displayOrder
    ) {
    }

    public record StatusResponse(Long id, String code, String name, String description,
                                 boolean isBlocking, boolean hiddenByDefault, boolean isDefault,
                                 boolean isArchivedState, boolean isDeletedState,
                                 boolean isBlacklistState, boolean isSystem, int displayOrder) {
        public static StatusResponse from(SupStatus s) {
            return new StatusResponse(s.getId(), s.getCode(), s.getName(), s.getDescription(),
                    s.isBlocking(), s.isHiddenByDefault(), s.isDefault(),
                    s.isArchivedState(), s.isDeletedState(), s.isBlacklistState(),
                    s.isSystem(), s.getDisplayOrder());
        }
    }

    public record CategoryResponse(Long id, String name, String description, boolean active) {
        public static CategoryResponse from(SupCategory c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.isActive());
        }
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            boolean active
    ) {
    }

    public record TagResponse(Long id, String name, String color, boolean active) {
        public static TagResponse from(SupTag t) {
            return new TagResponse(t.getId(), t.getName(), t.getColor(), t.isActive());
        }
    }

    public record TagRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 32) String color,
            boolean active
    ) {
    }

    public record PaymentTermResponse(Long id, String code, String name, Integer days,
                                      boolean active, boolean isSystem, int displayOrder) {
        public static PaymentTermResponse from(SupPaymentTerm t) {
            return new PaymentTermResponse(t.getId(), t.getCode(), t.getName(), t.getDays(),
                    t.isActive(), t.isSystem(), t.getDisplayOrder());
        }
    }

    public record PaymentTermRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String code,
            @NotBlank @Size(max = 100) String name,
            Integer days,
            boolean active,
            int displayOrder
    ) {
    }

    public record RatingCriterionResponse(Long id, String code, String name, BigDecimal weight,
                                          boolean active, boolean isSystem, int displayOrder) {
        public static RatingCriterionResponse from(SupRatingCriterion c) {
            return new RatingCriterionResponse(c.getId(), c.getCode(), c.getName(), c.getWeight(),
                    c.isActive(), c.isSystem(), c.getDisplayOrder());
        }
    }

    public record RatingCriterionRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String code,
            @NotBlank @Size(max = 100) String name,
            @NotNull @DecimalMin(value = "0.01") BigDecimal weight,
            boolean active,
            int displayOrder
    ) {
    }

    public record DocumentTypeResponse(Long id, String code, String name,
                                       boolean active, int displayOrder) {
        public static DocumentTypeResponse from(SupDocumentType t) {
            return new DocumentTypeResponse(t.getId(), t.getCode(), t.getName(),
                    t.isActive(), t.getDisplayOrder());
        }
    }
}
