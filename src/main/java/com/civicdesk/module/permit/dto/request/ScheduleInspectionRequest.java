package com.civicdesk.module.permit.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ScheduleInspectionRequest {
    private String assignedOfficerId;
    private LocalDate scheduledDate;
}