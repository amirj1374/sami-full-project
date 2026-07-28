package com.sami.app.crm.dto;

import com.sami.app.crm.domain.CustomerType;

/** Public representation of a {@link CustomerType}. */
public record CustomerTypeResponse(
        Long id, String code, String name, String description,
        boolean isDefault, boolean isSystem, boolean active, int displayOrder
) {
    public static CustomerTypeResponse from(CustomerType t) {
        return new CustomerTypeResponse(t.getId(), t.getCode(), t.getName(), t.getDescription(),
                t.isDefault(), t.isSystem(), t.isActive(), t.getDisplayOrder());
    }
}
