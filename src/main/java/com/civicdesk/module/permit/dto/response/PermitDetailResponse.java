package com.civicdesk.module.permit.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class PermitDetailResponse {
    private String              permitId;
    private String              permitType;
    private String              status;
    private LocalDate           applicationDate;
    private String              propertyAddress;
    private String              ward;
    private String              zone;
    private Integer             validityPeriod;
    private LocalDate           validFrom;
    private LocalDate           validUntil;
    private Double              fee;
    private LocalDate           decisionDate;
    private String              rejectionReason;
    private Map<String, Object> permitDetails;
}