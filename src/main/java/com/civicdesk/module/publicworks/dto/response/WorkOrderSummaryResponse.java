package com.civicdesk.module.publicworks.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class WorkOrderSummaryResponse {
    private String workOrderId;
    private String projectName;
    private String category;
    private String ward;
    private String status;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private BigDecimal budgetAllocated;
    private BigDecimal budgetConsumedTotal;
}
