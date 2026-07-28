package com.sami.app.crm.dto;

import com.sami.app.crm.domain.Segment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Request/response payloads for stored segments (marketing / saved filters). */
public final class SegmentDtos {

    private SegmentDtos() {
    }

    public record SegmentRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            /** The listing filter shape as a JSON object. */
            @NotNull Map<String, Object> filter
    ) {
    }

    public record SegmentResponse(Long id, String name, String description,
                                  Map<String, Object> filter) {
        public static SegmentResponse from(Segment s) {
            return new SegmentResponse(s.getId(), s.getName(), s.getDescription(), s.getFilter());
        }
    }
}
