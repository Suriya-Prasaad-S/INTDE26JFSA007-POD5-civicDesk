package com.civicdesk.module.serviceRequest.dto.request;

import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Body for PUT /civicDesk/serviceRequest/updateService/{serviceId}.
 * Updates the editable fields of a catalog service (or deactivates it via {@code status});
 * {@code departmentId} and {@code category} are fixed at creation and not editable here.
 */
public record UpdateServiceRequest(

        @NotBlank(message = "serviceName is required")
        String serviceName,

        @Positive(message = "processingDays must be greater than 0")
        int processingDays,

        @PositiveOrZero(message = "fee must be zero or greater")
        BigDecimal fee,

        @NotNull(message = "status must be Active or Inactive")
        ServiceStatus status
) {
}
