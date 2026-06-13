package com.civicdesk.module.grievance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update to a WORK action: change its status and (optionally) its content.
 * {@code status} is a short code: {@code O} / {@code IP} / {@code CM}.
 * Setting {@code CM} hands the grievance to the supervisor for review.
 */
@Data
public class GrievanceActionUpdateReq {

    @Size(max = 150, message = "grievanceActionTitle must be at most 150 characters")
    private String grievanceActionTitle;

    @Size(max = 2000, message = "actionDescription must be at most 2000 characters")
    private String actionDescription;

    @NotBlank(message = "status is required")
    private String status;
}
