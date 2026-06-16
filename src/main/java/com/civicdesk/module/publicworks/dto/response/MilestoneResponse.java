package com.civicdesk.module.publicworks.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MilestoneResponse {
    private String milestoneId;
    private String workOrderId;
    private String description;
    private LocalDate plannedDate;
    private LocalDate completedDate;
    private BigDecimal budgetConsumed;
    private String status;
    private String remarks;
}
