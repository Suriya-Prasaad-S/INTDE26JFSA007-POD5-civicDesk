package com.civicdesk.module.citizen.dto.response;

import java.time.LocalDate;

/**
 * Response payload for GET /getProfile/{citizenId}.
 *
 * <p>{@code nationalIdNumber} is returned masked (e.g. {@code IND-****7890}); the full value is
 * never exposed. {@code gender} and {@code status} are serialised as their String names.
 */
public record CitizenProfileResponse(
        String citizenId,
        String name,
        LocalDate dateOfBirth,
        String gender,
        String nationalIdNumber,
        String address,
        String ward,
        String zone,
        String email,
        String phone,
        String status
) {
}
