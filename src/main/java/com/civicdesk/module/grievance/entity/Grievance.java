package com.civicdesk.module.grievance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "grievances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grievance {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String grievanceId;

    @Column(length = 36, nullable = false, updatable = false)
    private String citizenId;

    @Column(length = 36)
    private String assignedToId;

    @Column(nullable = false)
    private String grievanceTitle;

    @Column(length = 30, nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(length = 50)
    private String ward;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submissionDate;

    @Column(length = 5, nullable = false)
    private String escalationLevel = "L1";

    @Column(length = 20, nullable = false)
    private String status = "Open";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (grievanceId == null) {
            grievanceId = UUID.randomUUID().toString();
        }
        if (submissionDate == null) {
            submissionDate = LocalDateTime.now();
        }
    }
}