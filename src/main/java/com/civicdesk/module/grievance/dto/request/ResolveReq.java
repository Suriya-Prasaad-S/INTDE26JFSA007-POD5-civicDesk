package com.civicdesk.module.grievance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Supervisor payload to mark a grievance Resolved (message is required). */
@Data
public class ResolveReq {

    @NotBlank(message = "message is required")
    @Size(max = 1000, message = "message must be at most 1000 characters")
    private String message;
}
