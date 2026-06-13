package com.civicdesk.module.serviceRequest.entity.external;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * TEMPORARY placeholder for the {@code departments} table owned by the IAM module.
 *
 * <p>This module only needs departments to exist so its {@code service_catalog} rows can
 * carry a real foreign key. The IAM team owns the real entity; when their module lands,
 * delete this class (and the rest of the {@code entity/external} package) and repoint the
 * {@code @ManyToOne} in {@link com.civicdesk.module.serviceRequest.entity.ServiceCatalog}
 * at their entity. See {@code docs/serviceRequest-module.md}.</p>
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @Column(length = 50)
    private String departmentId;

    @Column(nullable = false, length = 150)
    private String departmentName;

    @Column(nullable = false, length = 150)
    private String email;

    public Department() {
        // JPA + seeder construction
    }

    public Department(String departmentId, String departmentName, String email) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.email = email;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
