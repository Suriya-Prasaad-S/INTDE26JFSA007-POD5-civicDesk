package com.civicdesk.module.permit.entity;

import com.civicdesk.module.permit.enums.InspectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inspection {

    @Id
    @Column(name = "inspectionId", length = 36)
    private String inspectionId;

    @Column(name = "permitId", nullable = false, length = 36)
    private String permitId;

    @Column(name = "assignedOfficerId", nullable = false, length = 36)
    private String assignedOfficerId;

    @Column(name = "scheduledDate", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "conductedDate")
    private LocalDate conductedDate;

    @Column(name = "outcome", length = 25)
    private String outcome;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "gpsCoordinates", length = 60)
    private String gpsCoordinates;

    @Column(name = "photoPath", length = 255)
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InspectionStatus status;

    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = InspectionStatus.Scheduled;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}