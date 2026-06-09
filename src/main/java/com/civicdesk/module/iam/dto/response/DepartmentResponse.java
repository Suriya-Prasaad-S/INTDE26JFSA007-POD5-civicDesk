package com.civicdesk.module.iam.dto.response;

import com.civicdesk.module.iam.entity.Department;

public class DepartmentResponse {

    private String departmentId;
    private String name;
    private String departmentSupervisorId;

    public DepartmentResponse() {
    }

    public DepartmentResponse(String departmentId, String name, String departmentSupervisorId) {
        this.departmentId = departmentId;
        this.name = name;
        this.departmentSupervisorId = departmentSupervisorId;
    }

    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d.getDepartmentId(), d.getName(), d.getDepartmentSupervisorId());
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
