package com.civicdesk.module.serviceRequest.dto.response;

import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response for a newly submitted service request, including the snapshotted fee,
 * the auto-assigned officer and the computed expected completion date.
 */
public record SubmitRequestResponse(
        String message,
        String requestId,
        String citizenId,
        String serviceId,
        String serviceName,
        LocalDate submissionDate,
        String assignedOfficerId,
        BigDecimal fee,
        LocalDate expectedCompletionDate,
        RequestStatus status
) {
}
