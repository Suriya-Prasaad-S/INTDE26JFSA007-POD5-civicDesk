package com.civicdesk.module.permit.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class InspectionResponse {
    private String    inspectionId;
    private String    permitId;
    private String    permitType;
    private String    propertyAddress;
    private String    citizenName;
    private String    assignedOfficerId;
    private LocalDate scheduledDate;
    private LocalDate conductedDate;
    private String    outcome;
    private String    remarks;
    private String    gpsCoordinates;
    private String    photoPath;
    private String    status;
}