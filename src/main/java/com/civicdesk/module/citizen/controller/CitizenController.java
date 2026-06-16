package com.civicdesk.module.citizen.controller;

import com.civicdesk.module.citizen.dto.request.RegisterCitizenRequest;
import com.civicdesk.module.citizen.dto.request.UpdateCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.UpdateCitizenStatusRequest;
import com.civicdesk.module.citizen.dto.response.CitizenProfileResponse;
import com.civicdesk.module.citizen.dto.response.CitizenSummaryResponse;
import com.civicdesk.module.citizen.service.CitizenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Citizen profile endpoints under base path {@code /civicDesk/citizenProfile}.
 *
 * <p>GET endpoints return the response DTO; POST/PUT return a {@code {"message": …}} acknowledgement.
 * Registration returns the message only (the generated id is fetched via the listing endpoints).
 * {@code status} values on the API are single-character codes (A/V/F).
 */
@RestController
@RequestMapping("/civicDesk/citizenProfile")
public class CitizenController {

    private final CitizenService citizenService;

    public CitizenController(CitizenService citizenService) {
        this.citizenService = citizenService;
    }

    /** #1 — POST /registerCitizen (public). Returns 201 with a message only. */
    @PostMapping("/registerCitizen")
    public ResponseEntity<Map<String, Object>> registerCitizen(
            @Valid @RequestBody RegisterCitizenRequest request) {
        citizenService.registerCitizen(request);
        return ResponseEntity.status(201).body(message("Citizen registered successfully"));
    }

    /** #2 — GET /getProfile/{citizenId}. National ID is returned masked by the service. */
    @GetMapping("/getProfile/{citizenId}")
    public ResponseEntity<CitizenProfileResponse> getProfile(@PathVariable String citizenId) {
        return ResponseEntity.ok(citizenService.getProfile(citizenId));
    }

    /** #3 — PUT /updateProfile/{citizenId}. Patches the mutable fields only. */
    @PutMapping("/updateProfile/{citizenId}")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @PathVariable String citizenId,
            @Valid @RequestBody UpdateCitizenProfileRequest request) {
        citizenService.updateProfile(citizenId, request);
        return ResponseEntity.ok(message("Citizen profile updated successfully"));
    }

    /** #4 — PUT /updateStatus/{citizenId}. Enforces the allowed status transitions (codes A/V/F). */
    @PutMapping("/updateStatus/{citizenId}")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String citizenId,
            @Valid @RequestBody UpdateCitizenStatusRequest request) {
        citizenService.updateStatus(citizenId, request);
        return ResponseEntity.ok(message("Citizen status updated successfully"));
    }

    /** #10 — GET /getCitizensByWard/{ward}. Returns an empty list when the ward has no citizens. */
    @GetMapping("/getCitizensByWard/{ward}")
    public ResponseEntity<List<CitizenSummaryResponse>> getCitizensByWard(@PathVariable String ward) {
        return ResponseEntity.ok(citizenService.getCitizensByWard(ward));
    }

    /** GET /getAllCitizens — optional listing of every citizen (summary view). */
    @GetMapping("/getAllCitizens")
    public ResponseEntity<List<CitizenSummaryResponse>> getAllCitizens() {
        return ResponseEntity.ok(citizenService.getAllCitizens());
    }

    private static Map<String, Object> message(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        return body;
    }
}
