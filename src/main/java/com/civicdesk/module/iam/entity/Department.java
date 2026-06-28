package com.civicdesk.module.iam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @Column(name = "departmentId", length = 10, updatable = false, nullable = false)
    private String departmentId;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "departmentSupervisorId", length = 36)
    private String departmentSupervisorId;

    public Department() {
    }

    public Department(String name) {
        this.name = name;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartmentSupervisorId() {
        return departmentSupervisorId;
    }

    public void setDepartmentSupervisorId(String departmentSupervisorId) {
        this.departmentSupervisorId = departmentSupervisorId;
    }
}
