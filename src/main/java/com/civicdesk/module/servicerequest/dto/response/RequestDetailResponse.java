package com.civicdesk.module.serviceRequest.dto.response;

import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Full details of a single service request including its uploaded documents, returned by
 * getRequest. {@code feeSnapshot} is the fee captured at submission time.
 */
public record RequestDetailResponse(
        String requestId,
        String serviceName,
        String citizenId,
        RequestStatus status,
        BigDecimal feeSnapshot,
        LocalDate expectedCompletionDate,
        List<DocumentItemResponse> documents
) {
}
