package com.civicdesk.module.grievance.dto.response;

import java.time.LocalDateTime;

import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Citizen-facing view of a single grievance (internal ids are not exposed). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceResponse {

    private String grievanceId;
    private Category category;
    private String grievanceTitle;
    private String description;
    private String ward;
    private GrievanceStatus status;
    private EscalationLevel escalationLevel;
    private LocalDateTime submissionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
