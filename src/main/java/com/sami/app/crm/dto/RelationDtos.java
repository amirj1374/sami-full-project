package com.sami.app.crm.dto;

import com.sami.app.crm.domain.CustomerRelation;
import com.sami.app.crm.domain.RelationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request/response payloads for customer relationships. */
public final class RelationDtos {

    private RelationDtos() {
    }

    public record RelationTypeResponse(Long id, String code, String name,
                                       boolean isSystem, int displayOrder) {
        public static RelationTypeResponse from(RelationType t) {
            return new RelationTypeResponse(t.getId(), t.getCode(), t.getName(),
                    t.isSystem(), t.getDisplayOrder());
        }
    }

    public record RelationRequest(
            @NotNull(message = "Related customer is required") Long relatedCustomerId,
            @NotNull(message = "Relation type is required") Long relationTypeId,
            @Size(max = 255) String note
    ) {
    }

    /**
     * One relation as seen from a given customer's perspective: {@code other}
     * is the counterpart regardless of which side owns the row.
     */
    public record RelationResponse(
            Long id,
            RelationTypeResponse relationType,
            Long otherCustomerId,
            String otherCustomerCode,
            String otherDisplayName,
            String note,
            boolean outgoing
    ) {
        public static RelationResponse from(CustomerRelation r, Long perspectiveCustomerId) {
            boolean outgoing = r.getCustomer().getId().equals(perspectiveCustomerId);
            var other = outgoing ? r.getRelatedCustomer() : r.getCustomer();
            return new RelationResponse(
                    r.getId(),
                    RelationTypeResponse.from(r.getRelationType()),
                    other.getId(),
                    other.getCustomerCode(),
                    other.getDisplayName(),
                    r.getNote(),
                    outgoing);
        }
    }
}
