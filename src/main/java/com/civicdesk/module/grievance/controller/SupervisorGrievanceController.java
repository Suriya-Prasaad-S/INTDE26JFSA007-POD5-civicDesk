package com.civicdesk.module.grievance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.module.grievance.dto.request.AssignFieldOfficerReq;
import com.civicdesk.module.grievance.dto.request.ResolveReq;
import com.civicdesk.module.grievance.service.SupervisorGrievanceService;

import jakarta.validation.Valid;

/** Department-supervisor grievance endpoints. All require the {@code DS} role. */
@RestController
@RequestMapping("/grievance")
@PreAuthorize("hasRole('DS')")
public class SupervisorGrievanceController {

    private final SupervisorGrievanceService supervisorGrievanceService;

    public SupervisorGrievanceController(SupervisorGrievanceService supervisorGrievanceService) {
        this.supervisorGrievanceService = supervisorGrievanceService;
    }

    @GetMapping("getDepartmentGrievances")
    public ResponseEntity<ApiResponse> getDepartmentGrievances() {
        return ResponseEntity.ok(ApiResponse.data(supervisorGrievanceService.getDepartmentGrievances()));
    }

    @PostMapping("assignFieldOfficer/{grievanceId}")
    public ResponseEntity<ApiResponse> assignFieldOfficer(
            @PathVariable String grievanceId, @Valid @RequestBody AssignFieldOfficerReq req) {
        return ResponseEntity.ok(ApiResponse.of(
                "Field officer assigned successfully",
                supervisorGrievanceService.assignFieldOfficer(grievanceId, req)));
    }

    @PostMapping("resolveGrievance/{grievanceId}")
    public ResponseEntity<ApiResponse> resolveGrievance(
            @PathVariable String grievanceId, @Valid @RequestBody ResolveReq req) {
        return ResponseEntity.ok(ApiResponse.of(
                "Grievance resolved successfully",
                supervisorGrievanceService.resolveGrievance(grievanceId, req)));
    }

    @GetMapping("viewDepartmentGrievance/{grievanceId}")
    public ResponseEntity<ApiResponse> viewDepartmentGrievance(@PathVariable String grievanceId) {
        return ResponseEntity.ok(ApiResponse.data(
                supervisorGrievanceService.viewDepartmentGrievance(grievanceId)));
    }
}
