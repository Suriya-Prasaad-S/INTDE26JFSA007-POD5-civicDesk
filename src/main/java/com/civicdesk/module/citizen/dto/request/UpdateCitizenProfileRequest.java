package com.civicdesk.module.citizen.dto.request;

import jakarta.validation.constraints.Pattern;

/**
 * Body for PUT /updateProfile/{citizenId}.
 *
 * <p>All fields are optional; the service rejects a request with no updatable fields (400).
 * Email, gender, date of birth and national ID are intentionally NOT updatable through this
 * endpoint. {@code phone}, when supplied, must still be exactly 10 digits ({@code @Pattern}
 * allows null, so omitting it leaves the stored value unchanged).
 */
public record UpdateCitizenProfileRequest(
        String name,
        String address,
        String ward,
        String zone,
        @Pattern(regexp = "\\d{10}", message = "phone must be exactly 10 digits") String phone
) {
}
