package com.civicdesk.module.serviceRequest.dto.response;

import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;

import java.time.LocalDate;

/**
 * A service request as listed by getRequestsByCitizen (the citizen's personal tracker view).
 */
public record CitizenRequestItemResponse(
        String requestId,
        String serviceName,
        RequestStatus status,
        LocalDate expectedCompletionDate
) {
}
