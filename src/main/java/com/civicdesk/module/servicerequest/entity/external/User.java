package com.civicdesk.module.serviceRequest.entity.external;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * TEMPORARY placeholder for the {@code users} table owned by the IAM module.
 *
 * <p>This module needs users to exist so officers can be auto-assigned (role
 * {@code Officer}) and so a citizen's account status (Active / Flagged) can be checked.
 * The IAM team owns the real entity; when their module lands, delete this class and
 * repoint the {@code @ManyToOne assignedOfficer} in
 * {@link com.civicdesk.module.serviceRequest.entity.ServiceRequest} at their entity.
 * See {@code docs/serviceRequest-module.md}.</p>
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 50)
    private String userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(length = 15)
    private String phone;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(length = 50)
    private String departmentId;

    @Column(length = 20)
    private String status;

    public User() {
        // JPA + seeder construction
    }

    public User(String userId, String name, String email, String phone,
                String role, String departmentId, String status) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.departmentId = departmentId;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
