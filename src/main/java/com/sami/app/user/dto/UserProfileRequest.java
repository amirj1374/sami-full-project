package com.sami.app.user.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;

/**
 * Profile payload embedded in user create/update requests. Standard fields carry
 * declarative constraints; {@code customFields} is validated dynamically against
 * the active {@code ProfileFieldDefinition}s.
 */
public record UserProfileRequest(

        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Size(max = 160) String displayName,
        @Size(max = 32) String nationalCode,
        @Size(max = 32) String employeeCode,
        @Size(max = 32) String phoneNumber,
        @Size(max = 16) String gender,
        LocalDate birthDate,
        @Size(max = 500) String address,
        @Size(max = 2000) String notes,
        Map<String, Object> customFields
) {

    /** Empty payload used when a request omits the profile entirely. */
    public static UserProfileRequest empty() {
        return new UserProfileRequest(null, null, null, null, null, null, null, null, null, null, Map.of());
    }
}
