package com.sami.app.crm.dto;

import com.sami.app.crm.domain.CustomerContact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A phone/email entry, used in both request and response payloads. */
public record ContactDto(
        Long id,

        @NotNull(message = "Contact kind is required")
        CustomerContact.Kind kind,

        @NotBlank(message = "Contact value is required")
        @Size(max = 255)
        String value,

        @Size(max = 60)
        String label,

        boolean isDefault
) {
    public static ContactDto from(CustomerContact c) {
        return new ContactDto(c.getId(), c.getKind(), c.getValue(), c.getLabel(), c.isDefault());
    }
}
