package com.civicdesk.module.citizen.dto.response;

/**
 * Lightweight view of a citizen for officer listings (ward listing, pending-verification queue).
 * {@code userId} and {@code name} come from the IAM {@code User}; {@code ward} and {@code status}
 * come from {@code CitizenProfile}.
 */
public record CitizenSummaryResponse(
        String userId,
        String name,
        String ward,
        String status
) {
}
