package com.sami.app.crm.dto;

import java.util.List;

/** One potential duplicate: which customer matched and on which identifiers. */
public record DuplicateMatchResponse(
        Long customerId,
        String customerCode,
        String displayName,
        List<String> matchedOn
) {
}
