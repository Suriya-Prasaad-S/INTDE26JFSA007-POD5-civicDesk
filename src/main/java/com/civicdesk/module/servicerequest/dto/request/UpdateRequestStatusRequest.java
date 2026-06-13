package com.civicdesk.module.serviceRequest.dto.request;

import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Body for PUT /civicDesk/serviceRequest/updateRequestStatus/{requestId}.
 *
 * <p>{@code remarks} is accepted for the caller's convenience but is not persisted: the
 * ER schema has no column for it. Capture status-change history in a dedicated audit table
 * if that becomes a requirement.</p>
 */
public record UpdateRequestStatusRequest(

        @NotNull(message = "newStatus is required")
        RequestStatus newStatus,

        String remarks
) {
}
