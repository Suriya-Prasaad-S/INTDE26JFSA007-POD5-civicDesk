package com.civicdesk.module.grievance.controller;

import org.springframework.http.HttpStatus;
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
import com.civicdesk.module.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceDetailsUpdateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceReopenReq;
import com.civicdesk.module.grievance.service.CitizenGrievanceService;

import jakarta.validation.Valid;

/** Citizen-facing grievance endpoints. All require the {@code CIT} role. */
@RestController
@RequestMapping("/grievance")
@PreAuthorize("hasRole('CIT')")
public class CitizenGrievanceController {

    private final CitizenGrievanceService citizenGrievanceService;

    public CitizenGrievanceController(CitizenGrievanceService citizenGrievanceService) {
        this.citizenGrievanceService = citizenGrievanceService;
    }

    @PostMapping("createGrievance")
    public ResponseEntity<ApiResponse> createGrievance(@Valid @RequestBody GrievanceCreateReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Grievance created successfully", citizenGrievanceService.createGrievance(req)));
    }

    @PutMapping("updateGrievanceDetails/{grievanceId}")
    public ResponseEntity<ApiResponse> updateGrievanceDetails(
            @PathVariable String grievanceId, @Valid @RequestBody GrievanceDetailsUpdateReq req) {
        return ResponseEntity.ok(ApiResponse.of(
                "Grievance updated successfully", citizenGrievanceService.updateGrievanceDetails(grievanceId, req)));
    }

    @GetMapping("getMyGrievances")
    public ResponseEntity<ApiResponse> getMyGrievances() {
        return ResponseEntity.ok(ApiResponse.data(citizenGrievanceService.getMyGrievances()));
    }

    @GetMapping("getGrievanceById/{grievanceId}")
    public ResponseEntity<ApiResponse> getGrievanceById(@PathVariable String grievanceId) {
        return ResponseEntity.ok(ApiResponse.data(citizenGrievanceService.getGrievanceById(grievanceId)));
    }

    @PostMapping("closeGrievance/{grievanceId}")
    public ResponseEntity<ApiResponse> closeGrievance(@PathVariable String grievanceId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Grievance closed successfully", citizenGrievanceService.closeGrievance(grievanceId)));
    }

    @PostMapping("reopenGrievance/{grievanceId}")
    public ResponseEntity<ApiResponse> reopenGrievance(
            @PathVariable String grievanceId, @Valid @RequestBody GrievanceReopenReq req) {
        return ResponseEntity.ok(ApiResponse.of(
                "Grievance reopened successfully", citizenGrievanceService.reopenGrievance(grievanceId, req)));
    }
}
