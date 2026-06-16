package com.civicdesk.module.permit.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class RenewalRequest {
    private String propertyAddress;
    private String ward;
    private String zone;
    private Integer validityPeriod;
    private Double fee;
    private Map<String, Object> permitDetails;
}