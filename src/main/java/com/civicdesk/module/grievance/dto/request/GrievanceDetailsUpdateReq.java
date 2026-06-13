package com.civicdesk.module.grievance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Citizen edit of the editable fields; allowed only while the grievance is Open. */
@Data
public class GrievanceDetailsUpdateReq {

    @NotBlank(message = "grievanceTitle is required")
    @Size(max = 150, message = "grievanceTitle must be at most 150 characters")
    private String grievanceTitle;

    @NotBlank(message = "description is required")
    @Size(max = 2000, message = "description must be at most 2000 characters")
    private String description;
}
