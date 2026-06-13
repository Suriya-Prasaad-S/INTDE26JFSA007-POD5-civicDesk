package com.civicdesk.module.grievance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Citizen's reason for reopening a resolved grievance. */
@Data
public class GrievanceReopenReq {

    @NotBlank(message = "reason is required")
    @Size(max = 1000, message = "reason must be at most 1000 characters")
    private String reason;
}
