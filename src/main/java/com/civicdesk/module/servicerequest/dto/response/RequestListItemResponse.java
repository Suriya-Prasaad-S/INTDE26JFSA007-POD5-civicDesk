package com.civicdesk.module.serviceRequest.dto.response;

import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;

import java.time.LocalDate;

/**
 * A service request as listed by getAllRequests (the dept/queue view).
 */
public record RequestListItemResponse(
        String requestId,
        String serviceName,
        String citizenId,
        RequestStatus status,
        String departmentId,
        LocalDate expectedCompletionDate
) {
}
