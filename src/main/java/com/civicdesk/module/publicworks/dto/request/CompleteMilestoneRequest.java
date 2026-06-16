package com.civicdesk.module.publicworks.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CompleteMilestoneRequest {
    private LocalDate completedDate;
    private BigDecimal budgetConsumed;
    private String remarks;
}
