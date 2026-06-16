package com.civicdesk.module.publicworks.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DelayedMilestoneResponse {
    private String milestoneId;
    private String workOrderId;
    private String projectName;
    private String description;
    private LocalDate plannedDate;
    private long daysOverdue;
    private String status;
    private String remarks;
}
