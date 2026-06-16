package com.civicdesk.module.publicworks.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "milestone")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Milestone {

    @Id
    @Column(name = "milestoneId", length = 36)
    private String milestoneId;

    @Column(name = "workOrderId", nullable = false, length = 36)
    private String workOrderId;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "plannedDate", nullable = false)
    private LocalDate plannedDate;

    @Column(name = "completedDate")
    private LocalDate completedDate;

    @Column(name = "budgetConsumed", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetConsumed = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "Pending";

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "isDeleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (budgetConsumed == null) budgetConsumed = BigDecimal.ZERO;
        if (status == null) status = "Pending";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
