package com.civicdesk.module.grievance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Supervisor payload to assign (or reassign) a field officer to a grievance. */
@Data
public class AssignFieldOfficerReq {

    @NotBlank(message = "fieldOfficerId is required")
    private String fieldOfficerId;

    @NotBlank(message = "message is required")
    @Size(max = 1000, message = "message must be at most 1000 characters")
    private String message;
}
