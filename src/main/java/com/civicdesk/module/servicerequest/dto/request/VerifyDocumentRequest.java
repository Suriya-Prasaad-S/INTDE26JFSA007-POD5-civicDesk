package com.civicdesk.module.serviceRequest.dto.request;

import com.civicdesk.module.serviceRequest.entity.enums.VerificationStatus;

/**
 * Body for PUT /civicDesk/serviceRequest/verifyDocument/{docId}.
 * Only {@code Verified} or {@code Rejected} are accepted (validated in the service);
 * {@code Pending} is the initial state and cannot be set here.
 */
public record VerifyDocumentRequest(
        VerificationStatus verificationStatus
) {
}
