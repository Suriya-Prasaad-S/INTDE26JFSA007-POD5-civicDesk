package com.civicdesk.module.iam.dto.response;

import com.civicdesk.module.iam.entity.User;

import java.time.LocalDateTime;

/** Read model for a user — never exposes the password hash. */
public class UserResponse {

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String departmentId;
    private String status;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(String userId, String name, String email, String phone,
                        String role, String departmentId, String status, LocalDateTime createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.departmentId = departmentId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getDepartmentId(),
                user.getStatus(),
                user.getCreatedAt());
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
