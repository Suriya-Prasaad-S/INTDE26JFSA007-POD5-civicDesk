package com.civicdesk.module.grievance.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single grievance plus its full action timeline (oldest first).
 * Entities are never exposed — only the response DTOs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceDetailResponse {

    private GrievanceResponse grievance;
    private List<GrievanceActionResponse> actions;
}
