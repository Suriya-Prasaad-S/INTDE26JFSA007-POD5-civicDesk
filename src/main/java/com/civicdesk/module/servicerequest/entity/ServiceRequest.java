package com.civicdesk.module.serviceRequest.entity;

import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.entity.external.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A citizen's application for a catalog service. The fee and expected completion date
 * are snapshotted at submission time so later catalog edits do not affect open requests.
 *
 * <p>{@code citizenId}, {@code assignedOfficerId} and {@code departmentId} reference rows
 * owned by other modules (citizen / IAM). They are stored as plain id strings here - those
 * modules own the actual entities, so no JPA relationship is mapped.</p>
 */
@Entity
@Table(name = "service_request")
public class ServiceRequest {

    @Id
    @Column(length = 50)
    private String requestId;

    /** The submitting citizen. FK column {@code citizenId} → {@code citizen_profile}. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "citizenId", nullable = false)
    private CitizenProfile citizen;

    /** The requested catalog service. FK column {@code serviceId} → {@code service_catalog}. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "serviceId", nullable = false)
    private ServiceCatalog service;

    @Column(nullable = false)
    private LocalDate submissionDate;

    /** The auto-assigned officer. FK column {@code assignedOfficerId} → {@code users}. */
    @ManyToOne
    @JoinColumn(name = "assignedOfficerId")
    private User assignedOfficer;

    @Column(precision = 10, scale = 2)
    private BigDecimal fee;

    private LocalDate expectedCompletionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.Submitted;

    public ServiceRequest() {
        // JPA + service-layer construction
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public CitizenProfile getCitizen() {
        return citizen;
    }

    public void setCitizen(CitizenProfile citizen) {
        this.citizen = citizen;
    }

    public ServiceCatalog getService() {
        return service;
    }

    public void setService(ServiceCatalog service) {
        this.service = service;
    }

    public LocalDate getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDate submissionDate) {
        this.submissionDate = submissionDate;
    }

    public User getAssignedOfficer() {
        return assignedOfficer;
    }

    public void setAssignedOfficer(User assignedOfficer) {
        this.assignedOfficer = assignedOfficer;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public LocalDate getExpectedCompletionDate() {
        return expectedCompletionDate;
    }

    public void setExpectedCompletionDate(LocalDate expectedCompletionDate) {
        this.expectedCompletionDate = expectedCompletionDate;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }
}
