package com.civicdesk.module.iam.controller;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.module.iam.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only listing so an ADM can pick a valid departmentId when creating a supervisor. */
@RestController
@RequestMapping("/iam/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADM', 'DS')")
    public ResponseEntity<ApiResponse> getDepartments() {
        return ResponseEntity.ok(ApiResponse.data(departmentService.getAll()));
    }
}
