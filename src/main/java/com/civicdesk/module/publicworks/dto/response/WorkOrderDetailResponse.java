package com.civicdesk.module.publicworks.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class WorkOrderDetailResponse {
    private String workOrderId;
    private String projectName;
    private String category;
    private String ward;
    private String zone;
    private BigDecimal budgetAllocated;
    private BigDecimal budgetConsumedTotal;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;
    private String assignedContractorId;
    private String assignedEngineerId;
    private String status;
    private String remarks;
}
