package com.civicdesk.module.grievance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Field-officer (or no-FO supervisor) payload to create a WORK action on a grievance. */
@Data
public class GrievanceActionCreateReq {

    @NotBlank(message = "grievanceActionTitle is required")
    @Size(max = 150, message = "grievanceActionTitle must be at most 150 characters")
    private String grievanceActionTitle;

    @NotBlank(message = "actionDescription is required")
    @Size(max = 2000, message = "actionDescription must be at most 2000 characters")
    private String actionDescription;
}
