package com.civicdesk.module.citizen.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for PUT /updateStatus/{citizenId}. The value is validated against
 * Active / Verified / Flagged (and the allowed transition) in the service layer.
 */
public record UpdateCitizenStatusRequest(
        @NotBlank(message = "Missing required field: status") String status
) {
}
