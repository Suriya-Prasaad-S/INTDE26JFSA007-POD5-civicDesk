package com.civicdesk.module.iam.entity;

import com.civicdesk.common.id.NumericStringSequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// columnList values match the physical (camelCase) column names — this project uses
// PhysicalNamingStrategyStandardImpl, so columns are NOT snake_cased.
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_role",        columnList = "role"),
    @Index(name = "idx_users_status",      columnList = "status"),
    @Index(name = "idx_users_role_status", columnList = "role, status")
})
public class User {

    // Sequential numeric id rendered as a String: 10000001, 10000002, … (was a UUID).
    @Id
    @GeneratedValue(generator = "userIdSeq")
    @GenericGenerator(
            name = "userIdSeq",
            type = NumericStringSequenceGenerator.class,
            parameters = {
                @Parameter(name = "sequence_name", value = "user_id_seq"),
                @Parameter(name = "initial_value", value = "10000001"),
                @Parameter(name = "increment_size", value = "1"),
                @Parameter(name = "optimizer", value = "none")
            })
    @Column(name = "userId", length = 36, updatable = false, nullable = false)
    private String userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Null until the user sets a password (admin-created accounts start with no password). */
    @Column(name = "passwordHash", length = 255)
    private String passwordHash;

    /** False for admin-created accounts until the owner sets their own password on first login. */
    @Column(name = "isPasswordSet", nullable = false)
    private boolean passwordSet = false;

    @Column(length = 15)
    private String phone;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(name = "departmentId", length = 36)
    private String departmentId;

    @Column(nullable = false, length = 20)
    private String status = "A";

    @Column(name = "createdAt", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public User() {
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isPasswordSet() {
        return passwordSet;
    }

    public void setPasswordSet(boolean passwordSet) {
        this.passwordSet = passwordSet;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
