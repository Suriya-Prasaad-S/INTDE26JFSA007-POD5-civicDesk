package com.civicdesk.module.grievance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.module.grievance.dto.request.GrievanceActionCreateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceActionUpdateReq;
import com.civicdesk.module.grievance.service.FieldOfficerGrievanceService;

import jakarta.validation.Valid;

/**
 * Field-officer grievance endpoints. Listing/viewing my assignments is {@code FO}-only;
 * the WORK-action endpoints also allow {@code DS} (a supervisor doing the work directly in
 * a department that has no field officer).
 */
@RestController
@RequestMapping("/grievance")
@PreAuthorize("hasRole('FO')")
public class FieldOfficerGrievanceController {

    private final FieldOfficerGrievanceService fieldOfficerGrievanceService;

    public FieldOfficerGrievanceController(FieldOfficerGrievanceService fieldOfficerGrievanceService) {
        this.fieldOfficerGrievanceService = fieldOfficerGrievanceService;
    }

    @GetMapping("getAssignedGrievances")
    public ResponseEntity<ApiResponse> getAssignedGrievances() {
        return ResponseEntity.ok(ApiResponse.data(fieldOfficerGrievanceService.getAssignedGrievances()));
    }

    @GetMapping("viewAssignedGrievance/{grievanceId}")
    public ResponseEntity<ApiResponse> viewAssignedGrievance(@PathVariable String grievanceId) {
        return ResponseEntity.ok(ApiResponse.data(
                fieldOfficerGrievanceService.viewAssignedGrievance(grievanceId)));
    }

    @PostMapping("createGrievanceAction/{grievanceId}")
    @PreAuthorize("hasAnyRole('FO','DS')")
    public ResponseEntity<ApiResponse> createGrievanceAction(
            @PathVariable String grievanceId, @Valid @RequestBody GrievanceActionCreateReq req) {
        return ResponseEntity.ok(ApiResponse.of(
                "Work action created successfully",
                fieldOfficerGrievanceService.createGrievanceAction(grievanceId, req)));
    }

    @PutMapping("updateGrievanceAction/{actionId}")
    @PreAuthorize("hasAnyRole('FO','DS')")
    public ResponseEntity<ApiResponse> updateGrievanceAction(
            @PathVariable String actionId, @Valid @RequestBody GrievanceActionUpdateReq req) {
        return ResponseEntity.ok(ApiResponse.of(
                "Work action updated successfully",
                fieldOfficerGrievanceService.updateGrievanceAction(actionId, req)));
    }
}
