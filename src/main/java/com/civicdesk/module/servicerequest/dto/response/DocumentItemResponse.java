package com.civicdesk.module.serviceRequest.dto.response;

import com.civicdesk.module.serviceRequest.entity.enums.VerificationStatus;

import java.time.LocalDateTime;

/**
 * A single uploaded document, as listed by getDocuments and nested inside getRequest.
 */
public record DocumentItemResponse(
        String docId,
        String documentType,
        VerificationStatus verificationStatus,
        LocalDateTime uploadedDate
) {
}
