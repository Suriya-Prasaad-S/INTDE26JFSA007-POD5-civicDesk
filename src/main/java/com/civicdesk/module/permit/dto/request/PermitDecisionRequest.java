package com.civicdesk.module.permit.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermitDecisionRequest {
    private String decision;
    private String rejectionReason;
}