package com.civicdesk.module.permit.entity;

import com.civicdesk.module.permit.enums.PermitStatus;
import com.civicdesk.module.permit.enums.PermitType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "permit_application")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermitApplication {

    @Id
    @Column(name = "permitId", length = 36)
    private String permitId;

    @Column(name = "citizenId", nullable = false, length = 36)
    private String citizenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permitType", nullable = false, length = 30)
    private PermitType permitType;

    @Column(name = "applicationDate", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "propertyAddress", nullable = false, length = 255)
    private String propertyAddress;

    @Column(name = "ward", nullable = false, length = 50)
    private String ward;

    @Column(name = "zone", nullable = false, length = 50)
    private String zone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permitDetails", columnDefinition = "JSON")
    private Map<String, Object> permitDetails;

    @Column(name = "validityPeriod")
    private Integer validityPeriod;

    @Column(name = "validFrom")
    private LocalDate validFrom;

    @Column(name = "validUntil")
    private LocalDate validUntil;

    @Column(name = "fee")
    private Double fee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PermitStatus status;

    @Column(name = "rejectionReason", length = 255)
    private String rejectionReason;

    @Column(name = "reviewedBy", length = 36)
    private String reviewedBy;

    @Column(name = "decisionDate")
    private LocalDate decisionDate;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}