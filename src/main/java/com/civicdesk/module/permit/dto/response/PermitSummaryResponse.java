package com.civicdesk.module.permit.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PermitSummaryResponse {
    private String    permitId;
    private String    permitType;
    private String    propertyAddress;
    private String    status;
    private LocalDate applicationDate;
    private LocalDate validUntil;
    private String    citizenName;
}