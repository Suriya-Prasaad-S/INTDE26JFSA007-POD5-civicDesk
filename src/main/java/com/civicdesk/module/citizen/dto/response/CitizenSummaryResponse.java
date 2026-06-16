package com.civicdesk.module.citizen.dto.response;

/**
 * Item shape for the list returned by GET /getCitizensByWard/{ward} — a lightweight view of a
 * citizen (no sensitive fields).
 */
public record CitizenSummaryResponse(
        String citizenId,
        String name,
        String ward,
        String status
) {
}
