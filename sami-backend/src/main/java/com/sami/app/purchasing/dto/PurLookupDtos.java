package com.sami.app.purchasing.dto;

import com.sami.app.purchasing.domain.PurApprovalRule;
import com.sami.app.purchasing.domain.PurCancelReason;
import com.sami.app.purchasing.domain.PurIdentifierType;
import com.sami.app.purchasing.domain.PurStatus;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.inventory.domain.InventoryWarehouse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Responses and requests for the purchasing module's configurable lookups. */
public final class PurLookupDtos {

    private static final String SLUG = "^[a-z][a-z0-9-]{1,63}$";
    private static final String SLUG_MESSAGE = "Code must be a lowercase slug (letters, digits, dashes)";

    private PurLookupDtos() {
    }

    public record StatusResponse(
            Long id, String code, String name, String description,
            boolean allowsEditing, boolean allowsReceiving, boolean isTerminal,
            boolean isDraftState, boolean isPendingState, boolean isApprovedState,
            boolean isPartialState, boolean isCompletedState, boolean isCancelledState,
            boolean isRejectedState, boolean isSystem, int displayOrder
    ) {
        public static StatusResponse from(PurStatus s) {
            return new StatusResponse(s.getId(), s.getCode(), s.getName(), s.getDescription(),
                    s.isAllowsEditing(), s.isAllowsReceiving(), s.isTerminal(),
                    s.isDraftState(), s.isPendingState(), s.isApprovedState(),
                    s.isPartialState(), s.isCompletedState(), s.isCancelledState(),
                    s.isRejectedState(), s.isSystem(), s.getDisplayOrder());
        }
    }

    public record TypeResponse(
            Long id, String code, String name, String description, String numberPrefix,
            boolean isDefault, boolean active, boolean isSystem, boolean tenantOwned, int displayOrder
    ) {
        public static TypeResponse from(PurType t) {
            return new TypeResponse(t.getId(), t.getCode(), t.getName(), t.getDescription(),
                    t.getNumberPrefix(), t.isDefault(), t.isActive(), t.isSystem(),
                    t.getTenantId() != null, t.getDisplayOrder());
        }
    }

    public record TypeRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String code,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            @NotBlank @Size(max = 16) String numberPrefix,
            boolean active,
            int displayOrder
    ) {
    }

    public record CancelReasonResponse(Long id, String code, String name,
                                       boolean active, boolean isSystem, boolean tenantOwned,
                                       int displayOrder) {
        public static CancelReasonResponse from(PurCancelReason r) {
            return new CancelReasonResponse(r.getId(), r.getCode(), r.getName(),
                    r.isActive(), r.isSystem(), r.getTenantId() != null, r.getDisplayOrder());
        }
    }

    public record CancelReasonRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String code,
            @NotBlank @Size(max = 100) String name,
            boolean active,
            int displayOrder
    ) {
    }

    public record IdentifierTypeResponse(Long id, String code, String name,
                                         boolean satisfiesSerial, boolean satisfiesImei,
                                         boolean active, boolean isSystem, boolean tenantOwned,
                                         int displayOrder) {
        public static IdentifierTypeResponse from(PurIdentifierType t) {
            return new IdentifierTypeResponse(t.getId(), t.getCode(), t.getName(),
                    t.isSatisfiesSerial(), t.isSatisfiesImei(), t.isActive(),
                    t.isSystem(), t.getTenantId() != null, t.getDisplayOrder());
        }
    }

    public record IdentifierTypeRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String code,
            @NotBlank @Size(max = 100) String name,
            boolean satisfiesSerial,
            boolean satisfiesImei,
            boolean active,
            int displayOrder
    ) {
    }

    public record WarehouseResponse(Long id, String code, String name,
                                    boolean active, int displayOrder) {
        public static WarehouseResponse from(InventoryWarehouse w) {
            return new WarehouseResponse(w.getId(), w.getCode(), w.getName(),
                    w.isActive(), w.getDisplayOrder());
        }
    }

    public record ApprovalRuleResponse(Long id, String name, BigDecimal minAmount,
                                       boolean active, boolean tenantOwned) {
        public static ApprovalRuleResponse from(PurApprovalRule r) {
            return new ApprovalRuleResponse(r.getId(), r.getName(), r.getMinAmount(),
                    r.isActive(), r.getTenantId() != null);
        }
    }

    public record ApprovalRuleRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull @PositiveOrZero BigDecimal minAmount,
            boolean active
    ) {
    }
}
