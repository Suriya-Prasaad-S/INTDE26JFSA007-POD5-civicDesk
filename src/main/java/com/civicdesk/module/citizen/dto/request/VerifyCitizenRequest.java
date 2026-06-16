package com.civicdesk.module.citizen.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for {@code PUT /citizenProfile/{userId}/verify} — an officer verifies (or flags) a citizen.
 *
 * <p>{@code status} is a single-character citizen status code: {@code V} (Verified) or {@code F}
 * (Flagged). {@code A} (Active) is not a valid verify target. The verifier's identity is taken
 * from the JWT, not the body.
 */
public record VerifyCitizenRequest(
        @NotBlank(message = "status is required") String status
) {
}
