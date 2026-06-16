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
@Table(name = "work_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrder {

    @Id
    @Column(name = "workOrderId", length = 36)
    private String workOrderId;

    @Column(name = "projectName", nullable = false, length = 150)
    private String projectName;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "ward", nullable = false, length = 50)
    private String ward;

    @Column(name = "zone", length = 50)
    private String zone;

    @Column(name = "budgetAllocated", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetAllocated;

    @Column(name = "budgetConsumedTotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetConsumedTotal = BigDecimal.ZERO;

    @Column(name = "startDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "expectedEndDate", nullable = false)
    private LocalDate expectedEndDate;

    @Column(name = "actualEndDate")
    private LocalDate actualEndDate;

    @Column(name = "assignedContractorId", length = 36)
    private String assignedContractorId;

    @Column(name = "assignedEngineerId", length = 36)
    private String assignedEngineerId;

    @Column(name = "departmentId", length = 36)
    private String departmentId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "Planned";

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
        if (budgetConsumedTotal == null) budgetConsumedTotal = BigDecimal.ZERO;
        if (status == null) status = "Planned";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
