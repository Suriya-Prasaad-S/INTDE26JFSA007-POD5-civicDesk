package com.civicdesk.module.grievance.entity;

import com.civicdesk.common.id.NumericStringSequenceGenerator;
import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "grievances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grievance {

    // Sequential numeric id rendered as a String (e.g. 30000001), matching IAM's id strategy.
    @Id
    @GeneratedValue(generator = "grievanceIdSeq")
    @GenericGenerator(
            name = "grievanceIdSeq",
            type = NumericStringSequenceGenerator.class,
            parameters = {
                @Parameter(name = "sequence_name", value = "grievance_id_seq"),
                @Parameter(name = "initial_value", value = "30000001"),
                @Parameter(name = "increment_size", value = "1"),
                @Parameter(name = "optimizer", value = "none")
            })
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
        if (submissionDate == null) {
            submissionDate = LocalDateTime.now();
        }
    }
}
