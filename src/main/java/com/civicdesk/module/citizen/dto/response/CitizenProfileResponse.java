package com.civicdesk.module.citizen.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response payload for {@code GET /citizenProfile/me}.
 *
 * <p>{@code userId}, {@code name}, {@code email} and {@code phone} come from the IAM {@code User};
 * the remaining fields come from {@code CitizenProfile}. {@code nationalIdNumber} is returned
 * masked (e.g. {@code ****7890}); the full value is never exposed. {@code gender} and
 * {@code status} are serialised as their String names/codes. The extra fields are {@code null}
 * until the citizen completes their profile.
 */
public record CitizenProfileResponse(
        String userId,
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String nationalIdNumber,
        String address,
        String ward,
        String zone,
        String status,
        String verifiedBy,
        LocalDateTime verifiedAt,
        LocalDateTime createdAt
) {
}
