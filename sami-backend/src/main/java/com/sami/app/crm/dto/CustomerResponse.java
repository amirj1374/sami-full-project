package com.sami.app.crm.dto;

import com.sami.app.crm.domain.Customer;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** List-row representation of a {@link Customer}. */
public record CustomerResponse(
        Long id,
        String customerCode,
        String displayName,
        String firstName,
        String lastName,
        String companyName,
        CustomerTypeResponse type,
        CustomerStatusResponse status,
        CustomerSourceResponse source,
        List<CustomerTagResponse> tags,
        Long mergedIntoId,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        Instant archivedAt,
        Instant deletedAt
) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getCustomerCode(),
                c.getDisplayName(),
                c.getFirstName(),
                c.getLastName(),
                c.getCompanyName(),
                CustomerTypeResponse.from(c.getType()),
                CustomerStatusResponse.from(c.getStatus()),
                c.getSource() != null ? CustomerSourceResponse.from(c.getSource()) : null,
                c.getTags().stream()
                        .sorted(Comparator.comparing(t -> t.getName().toLowerCase()))
                        .map(CustomerTagResponse::from)
                        .toList(),
                c.getMergedInto() != null ? c.getMergedInto().getId() : null,
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getVersion(),
                c.getArchivedAt(),
                c.getDeletedAt()
        );
    }
}
