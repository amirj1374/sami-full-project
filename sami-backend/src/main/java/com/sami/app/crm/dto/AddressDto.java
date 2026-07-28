package com.sami.app.crm.dto;

import com.sami.app.crm.domain.CustomerAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A postal address entry, used in both request and response payloads. */
public record AddressDto(
        Long id,

        @Size(max = 60)
        String label,

        @NotBlank(message = "Address line is required")
        @Size(max = 300)
        String line,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String province,

        @Size(max = 20)
        String postalCode,

        boolean isDefault
) {
    public static AddressDto from(CustomerAddress a) {
        return new AddressDto(a.getId(), a.getLabel(), a.getLine(), a.getCity(),
                a.getProvince(), a.getPostalCode(), a.isDefault());
    }
}
