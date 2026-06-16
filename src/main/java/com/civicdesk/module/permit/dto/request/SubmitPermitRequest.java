package com.civicdesk.module.permit.dto.request;

import com.civicdesk.module.permit.enums.PermitType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SubmitPermitRequest {
    private PermitType permitType;
    private String propertyAddress;
    private String ward;
    private String zone;
    private Integer validityPeriod;
    private Double fee;
    private Map<String, Object> permitDetails;
    private String citizenId;
}