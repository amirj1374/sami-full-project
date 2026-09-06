package com.sami.app.contact.dto;

import java.time.Instant;
import java.util.List;

public final class ContactDtos {
    private ContactDtos() { }

    public record ContactResponse(Long id, String identityType, String displayName,
                                  String firstName, String lastName, String companyName,
                                  String nationalCode, String legalIdentifier, String taxNumber,
                                  String email, String phone, boolean active, Long mergedIntoId,
                                  List<String> roles, Instant createdAt, Instant updatedAt) { }

    public record LegacyMappingResponse(Long id, String legacyType, Long legacyId,
                                        String matchMethod, boolean reviewRequired,
                                        Long contactId) { }

    public record RoleRequest(String role, Long legacyId) { }

    public record MergeResponse(Long sourceId, Long targetId, int mappingsMoved,
                                boolean sourcePreserved, String auditAction) { }
}
