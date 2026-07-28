package com.sami.app.crm.dto;

import com.sami.app.crm.domain.CustomerTag;

/** Public representation of a {@link CustomerTag}. */
public record CustomerTagResponse(
        Long id, String name, String color, String description, boolean active
) {
    public static CustomerTagResponse from(CustomerTag t) {
        return new CustomerTagResponse(t.getId(), t.getName(), t.getColor(),
                t.getDescription(), t.isActive());
    }
}
