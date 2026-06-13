package com.civicdesk.module.serviceRequest.dto.request;

import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body for POST /civicDesk/serviceRequest/createService/v1.0.
 */
public record CreateServiceRequest(

        @NotBlank(message = "serviceName is required")
        String serviceName,

        @NotBlank(message = "departmentId is required")
        String departmentId,

        @NotNull(message = "category is required")
        ServiceCategory category,

        @Positive(message = "processingDays must be greater than 0")
        int processingDays,

        List<String> requiredDocuments,

        @PositiveOrZero(message = "fee must be zero or greater")
        BigDecimal fee
) {
}
