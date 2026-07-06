package com.civicdesk.module.permit.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PermitAnalyticsRequest {

    private LocalDate fromDate;

    private LocalDate toDate;
}