package com.civicdesk.module.serviceRequest.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for POST /civicDesk/serviceRequest/submitRequest/v1.0.
 * The system snapshots the fee and computes the completion date from the catalog,
 * so only the citizen and the chosen service are supplied.
 */
public record SubmitServiceRequest(

        @NotBlank(message = "citizenId is required")
        String citizenId,

        @NotBlank(message = "serviceId is required")
        String serviceId
) {
}
