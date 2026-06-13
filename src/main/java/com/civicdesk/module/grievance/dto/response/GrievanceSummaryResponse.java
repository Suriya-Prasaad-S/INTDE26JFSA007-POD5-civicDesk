package com.civicdesk.module.grievance.dto.response;

import java.time.LocalDateTime;

import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lightweight row for the citizen's grievance list. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceSummaryResponse {

    private String grievanceId;
    private String grievanceTitle;
    private Category category;
    private GrievanceStatus status;
    private EscalationLevel escalationLevel;
    private LocalDateTime submissionDate;
}
