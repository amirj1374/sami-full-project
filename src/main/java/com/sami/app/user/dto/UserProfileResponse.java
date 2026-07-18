package com.sami.app.user.dto;

import com.sami.app.user.domain.UserProfile;

import java.time.LocalDate;
import java.util.Map;

/** Public representation of a {@link UserProfile}, including custom fields. */
public record UserProfileResponse(
        String firstName,
        String lastName,
        String displayName,
        String nationalCode,
        String employeeCode,
        String phoneNumber,
        String gender,
        LocalDate birthDate,
        boolean hasAvatar,
        String address,
        String notes,
        Map<String, Object> customFields
) {

    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDisplayName(),
                profile.getNationalCode(),
                profile.getEmployeeCode(),
                profile.getPhoneNumber(),
                profile.getGender(),
                profile.getBirthDate(),
                profile.getAvatarKey() != null,
                profile.getAddress(),
                profile.getNotes(),
                profile.getCustomFields()
        );
    }
}
