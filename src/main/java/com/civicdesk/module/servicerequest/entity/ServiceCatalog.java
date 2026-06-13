package com.civicdesk.module.serviceRequest.entity;

import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.external.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A government service offered in the catalog. Owned by the Admin / Government Admin role.
 * Citizens submit {@link ServiceRequest}s against an Active service.
 */
@Entity
@Table(name = "service_catalog")
public class ServiceCatalog {

    @Id
    @Column(length = 50)
    private String serviceId;

    @Column(nullable = false, length = 200)
    private String serviceName;

    /** The owning department. FK column {@code departmentId} → {@code departments}. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "departmentId", nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceCategory category;

    @Column(nullable = false)
    private int processingDays;

    /**
     * JSON array of required document types, e.g. ["NationalID","ResidenceProof"].
     * Not present in the ER diagram but defined in the technical spec; stored as TEXT.
     */
    @Column(columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(precision = 10, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ServiceStatus status = ServiceStatus.Active;

    public ServiceCatalog() {
        // JPA + service-layer construction
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    public int getProcessingDays() {
        return processingDays;
    }

    public void setProcessingDays(int processingDays) {
        this.processingDays = processingDays;
    }

    public String getRequiredDocuments() {
        return requiredDocuments;
    }

    public void setRequiredDocuments(String requiredDocuments) {
        this.requiredDocuments = requiredDocuments;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }
}
