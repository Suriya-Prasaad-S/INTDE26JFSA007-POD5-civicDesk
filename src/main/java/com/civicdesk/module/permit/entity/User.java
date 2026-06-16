package com.civicdesk.module.permit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "userId", length = 36)
    private String userId;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "passwordHash", length = 255)
    private String passwordHash;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "role", length = 30)
    private String role;

    @Column(name = "departmentId", length = 36)
    private String departmentId;

    @Column(name = "status", length = 20)
    private String status;
}