package com.sami.app.crm.dto;

import com.sami.app.crm.domain.CustomerSource;

/** Public representation of a {@link CustomerSource}. */
public record CustomerSourceResponse(
        Long id, String code, String name, boolean active, boolean isSystem, int displayOrder
) {
    public static CustomerSourceResponse from(CustomerSource s) {
        return new CustomerSourceResponse(s.getId(), s.getCode(), s.getName(),
                s.isActive(), s.isSystem(), s.getDisplayOrder());
    }
}
