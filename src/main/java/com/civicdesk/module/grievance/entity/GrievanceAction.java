package com.civicdesk.module.grievance.entity;

import com.civicdesk.module.grievance.enums.ActionStatus;
import com.civicdesk.module.grievance.enums.ActionType;

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

    /** Who created the action — resolved from the authenticated caller. */
    @Column(length = 50, nullable = false)
    private String takenById;

    /** Stored as a short code (enum name). */
    @Enumerated(EnumType.STRING)
    @Column(length = 5, nullable = false)
    private ActionType actionType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime actionDate;

    @Column(nullable = false, length = 150)
    private String grievanceActionTitle;

    /** The message (e.g. reason on reopen). Optional for some action types. */
    @Column(columnDefinition = "TEXT")
    private String actionDescription;

    /** Only meaningful for WORK actions; null for system/workflow rows. */
    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private ActionStatus status;

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
