package com.sami.app.crm.dto;

import com.sami.app.crm.domain.CustomerStatus;

/** Public representation of a {@link CustomerStatus}. */
public record CustomerStatusResponse(
        Long id, String code, String name, String description,
        boolean isBlocking, boolean hiddenByDefault, boolean isDefault,
        boolean isArchivedState, boolean isDeletedState, boolean isBlacklistState,
        boolean isSystem, int displayOrder
) {
    public static CustomerStatusResponse from(CustomerStatus s) {
        return new CustomerStatusResponse(s.getId(), s.getCode(), s.getName(), s.getDescription(),
                s.isBlocking(), s.isHiddenByDefault(), s.isDefault(),
                s.isArchivedState(), s.isDeletedState(), s.isBlacklistState(),
                s.isSystem(), s.getDisplayOrder());
    }
}
