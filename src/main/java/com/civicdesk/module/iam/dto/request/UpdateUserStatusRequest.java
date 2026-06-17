package com.civicdesk.module.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateUserStatusRequest {

    @NotBlank(message = "status is required")
    @Pattern(regexp = "(?i)(A|ACT|ACTIVE|I|INA|INACTIVE|S|SUS|SUSPENDED)",
            message = "status must be one of: A/Act/Active, I/Ina/Inactive, S/Sus/Suspended")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
