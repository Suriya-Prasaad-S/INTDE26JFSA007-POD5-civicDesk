package com.civicdesk.module.grievance.entity;

import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /** Owner — resolved from the authenticated caller, never the request body. */
    @Column(length = 50, nullable = false, updatable = false)
    private String citizenId;

    /** Owning department — resolved from {@link #category} at creation. */
    @Column(length = 50)
    private String departmentId;

    /** Current holder (supervisor at intake, field officer once assigned). */
    @Column(length = 50)
    private String assignedToId;

    /** Assigned field officer; null until assigned, retained through review. */
    @Column(length = 50)
    private String fieldOfficerId;

    @Column(nullable = false, length = 150)
    private String grievanceTitle;

    /** Fixed list; locked after submission. Stored as a short code (enum name). */
    @Enumerated(EnumType.STRING)
    @Column(length = 5, nullable = false, updatable = false)
    private Category category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(length = 50)
    private String ward;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submissionDate;

    /** Current handling tier; defaults to L1. */
    @Enumerated(EnumType.STRING)
    @Column(length = 5, nullable = false)
    private EscalationLevel escalationLevel = EscalationLevel.L2;

    @Enumerated(EnumType.STRING)
    @Column(length = 5, nullable = false)
    private GrievanceStatus status = GrievanceStatus.O;

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
