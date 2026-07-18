package com.sami.app.crm.dto;

import com.sami.app.crm.domain.Customer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** The full 360° customer payload (minus timeline/notes, which page separately). */
public record CustomerDetailResponse(
        CustomerResponse customer,
        String nationalCode,
        String passportNumber,
        LocalDate birthDate,
        String gender,
        String occupation,
        String taxNumber,
        boolean hasAvatar,
        List<ContactDto> contacts,
        List<AddressDto> addresses,
        Map<String, Object> preferences
) {

    public static CustomerDetailResponse from(Customer c) {
        return new CustomerDetailResponse(
                CustomerResponse.from(c),
                c.getNationalCode(),
                c.getPassportNumber(),
                c.getBirthDate(),
                c.getGender(),
                c.getOccupation(),
                c.getTaxNumber(),
                c.getAvatarKey() != null,
                c.getContacts().stream().map(ContactDto::from).toList(),
                c.getAddresses().stream().map(AddressDto::from).toList(),
                c.getPreferences()
        );
    }
}
