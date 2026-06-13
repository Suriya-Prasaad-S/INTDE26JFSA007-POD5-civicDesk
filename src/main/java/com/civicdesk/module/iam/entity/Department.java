package com.civicdesk.module.iam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A municipal department. A user with role DS is assigned to one
 * department, and that department points back to its supervisor (one-to-one)
 * via {@code department_supervisor_id}.
 *
 * <p>The id is a human-readable business key in the form {@code DPT01}, {@code DPT02}, …
 * assigned by {@code DataSeeder} (the only creator of departments) rather than a
 * generated UUID.
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @Column(name = "departmentId", length = 10, updatable = false, nullable = false)
    private String departmentId;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** userId of the DS who heads this department; null until one is assigned. */
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
