package com.civicdesk.module.citizen.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for PUT /{citizenId}/verifyDocument/{documentId}.
 *
 * <p>{@code verifiedBy} is supplied in the body for now; once Module 2.1 (IAM) is integrated it
 * will be auto-extracted from the JWT instead.
 */
public record VerifyDocumentRequest(
        @NotBlank(message = "Missing required field: verifiedBy") String verifiedBy,
        @NotBlank(message = "Missing required field: status") String status
) {
}
