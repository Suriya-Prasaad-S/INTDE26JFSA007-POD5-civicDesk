package com.civicdesk.module.publicworks.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PublicWorkOrderResponse {
    private String workOrderId;
    private String projectName;
    private String category;
    private String ward;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;
    private String status;
    private int totalMilestones;
    private int completedMilestones;
}
