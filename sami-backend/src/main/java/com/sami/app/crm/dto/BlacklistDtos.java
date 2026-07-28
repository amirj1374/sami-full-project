package com.sami.app.crm.dto;

import com.sami.app.crm.domain.BlacklistEntry;
import com.sami.app.crm.domain.BlacklistReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Request/response payloads for the customer blacklist. */
public final class BlacklistDtos {

    private BlacklistDtos() {
    }

    public record BlacklistReasonResponse(Long id, String code, String name,
                                          boolean active, boolean isSystem, int displayOrder) {
        public static BlacklistReasonResponse from(BlacklistReason r) {
            return new BlacklistReasonResponse(r.getId(), r.getCode(), r.getName(),
                    r.isActive(), r.isSystem(), r.getDisplayOrder());
        }
    }

    public record BlacklistRequest(
            @NotNull(message = "Reason is required") Long reasonId,
            @Size(max = 500) String note
    ) {
    }

    public record BlacklistEntryResponse(
            Long id,
            BlacklistReasonResponse reason,
            String note,
            Long createdBy,
            String createdByEmail,
            Instant createdAt,
            Instant liftedAt,
            Long liftedBy
    ) {
        public static BlacklistEntryResponse from(BlacklistEntry e) {
            return new BlacklistEntryResponse(e.getId(), BlacklistReasonResponse.from(e.getReason()),
                    e.getNote(), e.getCreatedBy(), e.getCreatedByEmail(), e.getCreatedAt(),
                    e.getLiftedAt(), e.getLiftedBy());
        }
    }
}
