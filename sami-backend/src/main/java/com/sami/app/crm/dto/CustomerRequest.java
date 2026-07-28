package com.sami.app.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Create/update payload for a customer. Contacts and addresses are full
 * replacement lists (PUT semantics).
 *
 * <p>{@code ignoreDuplicates} is the confirmed override: when false (default)
 * a create/update matching an enabled duplicate rule fails with 409 and the
 * matches; the client re-submits with the flag after explicit confirmation.
 * {@code expectedVersion} is the optimistic-concurrency check on update.
 */
public record CustomerRequest(

        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,

        @NotBlank(message = "Display name is required")
        @Size(max = 160)
        String displayName,

        @Size(max = 32) String nationalCode,
        @Size(max = 32) String passportNumber,
        LocalDate birthDate,
        @Size(max = 16) String gender,
        @Size(max = 120) String occupation,
        @Size(max = 160) String companyName,
        @Size(max = 64) String taxNumber,

        @NotNull(message = "Customer type is required")
        Long typeId,

        /** Optional on create (falls back to the default status). */
        Long statusId,

        Long sourceId,

        List<Long> tagIds,

        @Valid List<ContactDto> contacts,
        @Valid List<AddressDto> addresses,

        boolean ignoreDuplicates,
        Long expectedVersion
) {
}
