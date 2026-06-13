package com.civicdesk.module.grievance.dto.response;

import java.time.LocalDateTime;

import com.civicdesk.module.grievance.enums.ActionStatus;
import com.civicdesk.module.grievance.enums.ActionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One entry in a grievance's action timeline. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceActionResponse {

    private String actionId;
    private ActionType actionType;
    private String grievanceActionTitle;
    private String actionDescription;
    private ActionStatus status;
    private LocalDateTime actionDate;
    private String takenById;
}
