package com.civicdesk.module.publicworks.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BudgetSummaryResponse {
    private String workOrderId;
    private String projectName;
    private String ward;
    private BigDecimal budgetAllocated;
    private BigDecimal budgetConsumedTotal;
    private double utilizationPct;
    private String status;
}
