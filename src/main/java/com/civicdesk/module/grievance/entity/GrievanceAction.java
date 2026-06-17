package com.civicdesk.module.grievance.entity;

import com.civicdesk.common.id.NumericStringSequenceGenerator;
import com.civicdesk.module.grievance.enums.ActionStatus;
import com.civicdesk.module.grievance.enums.ActionType;

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
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDateTime;

@Entity
@Table(name = "grievance_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceAction {

    // Sequential numeric id rendered as a String (e.g. 40000001), matching IAM's id strategy.
    @Id
    @GeneratedValue(generator = "grievanceActionIdSeq")
    @GenericGenerator(
            name = "grievanceActionIdSeq",
            type = NumericStringSequenceGenerator.class,
            parameters = {
                @Parameter(name = "sequence_name", value = "grievance_action_id_seq"),
                @Parameter(name = "initial_value", value = "40000001"),
                @Parameter(name = "increment_size", value = "1"),
                @Parameter(name = "optimizer", value = "none")
            })
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
        if (actionDate == null) {
            actionDate = LocalDateTime.now();
        }
    }
}
