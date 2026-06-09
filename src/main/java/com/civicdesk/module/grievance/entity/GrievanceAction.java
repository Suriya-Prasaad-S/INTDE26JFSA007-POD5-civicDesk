package com.civicdesk.module.grievance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "grievance_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceAction {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String actionId;

    @Column(length = 36, nullable = false)
    private String grievanceId;

    @Column(length = 36, nullable = false)
    private String takenById;

    @Column(nullable = false, updatable = false)
    private LocalDateTime actionDate;

    @Column(nullable = false)
    private String grievanceActionTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String actionDescription;

    @Column(length = 500)
    private String grievanceProof;

    @Column(length = 20, nullable = false)
    private String status;

    @PrePersist
    protected void onCreate() {
        if (actionId == null) {
            actionId = UUID.randomUUID().toString();
        }
        if (actionDate == null) {
            actionDate = LocalDateTime.now();
        }
    }
}