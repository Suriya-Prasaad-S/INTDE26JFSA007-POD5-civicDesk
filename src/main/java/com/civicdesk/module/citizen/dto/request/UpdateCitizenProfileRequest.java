package com.civicdesk.module.citizen.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Body for {@code PUT /citizenProfile/me} — the citizen updates their own mutable extra fields.
 *
 * <p>All fields are optional; the service rejects a request with no updatable fields (400).
 * {@code name} / {@code email} / {@code phone} are {@code User} fields (edited through IAM, not
 * here); {@code dateOfBirth} / {@code gender} / {@code nationalIdNumber} are set once at profile
 * completion and are not editable here.
 */
public record UpdateCitizenProfileRequest(

        @Size(max = 255, message = "address must not exceed 255 characters")
        String address,

        @Size(max = 50, message = "ward must not exceed 50 characters")
        String ward,

        @Size(max = 50, message = "zone must not exceed 50 characters")
        String zone
) {
}
