package com.civicdesk.module.citizen.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for {@code PUT /citizenProfile/{userId}/verifyDocument/{documentId}}.
 *
 * <p>{@code status} is the target document status code (V/E/R). The verifier's identity is taken
 * from the JWT (the authenticated officer), not the body.
 */
public record VerifyDocumentRequest(
        @NotBlank(message = "status is required") String status
) {
}
