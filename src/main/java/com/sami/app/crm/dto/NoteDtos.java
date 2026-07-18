package com.sami.app.crm.dto;

import com.sami.app.crm.domain.CustomerNote;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Request/response payloads for customer notes. */
public final class NoteDtos {

    private NoteDtos() {
    }

    public record NoteRequest(

            @NotBlank(message = "Note body is required")
            @Size(max = 4000)
            String body,

            @NotNull(message = "Priority is required")
            CustomerNote.Priority priority,

            @NotNull(message = "Visibility is required")
            CustomerNote.Visibility visibility
    ) {
    }

    public record NoteResponse(
            Long id,
            String body,
            CustomerNote.Priority priority,
            CustomerNote.Visibility visibility,
            Long authorId,
            String authorEmail,
            Instant createdAt
    ) {
        public static NoteResponse from(CustomerNote n) {
            return new NoteResponse(n.getId(), n.getBody(), n.getPriority(), n.getVisibility(),
                    n.getAuthorId(), n.getAuthorEmail(), n.getCreatedAt());
        }
    }
}
