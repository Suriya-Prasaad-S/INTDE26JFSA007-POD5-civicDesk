package com.civicdesk.module.grievance.dto.response;

import java.util.List;

import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed view of a single grievance together with all actions that have been
 * taken against it, ordered chronologically.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceDetailResponse {

    private Grievance grievance;
    private List<GrievanceAction> actions;
}
