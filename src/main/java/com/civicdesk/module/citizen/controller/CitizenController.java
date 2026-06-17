package com.civicdesk.module.citizen.controller;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.module.citizen.dto.request.CompleteCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.UpdateCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.VerifyCitizenRequest;
import com.civicdesk.module.citizen.service.CitizenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Citizen profile endpoints under base path {@code /citizenProfile} (served below the application
 * context path {@code /civicDesk}). The caller is always identified by the JWT.
 *
 * <p>Citizen-facing endpoints ({@code /me}) require role {@code CIT}; officer endpoints
 * (pending list, verify) require {@code FO}/{@code DS}/{@code ADM}. All responses use the shared
 * {@link ApiResponse} envelope.
 */
@RestController
@RequestMapping("/citizenProfile")
public class CitizenController {

    private final CitizenService citizenService;

    public CitizenController(CitizenService citizenService) {
        this.citizenService = citizenService;
    }

    // --- Citizen-facing (role CIT) ---

    /** View own profile + verification state; creates a stub on first call. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.data(citizenService.getMyProfile()));
    }

    /** Complete own profile (extra fields) after being verified. */
    @PostMapping("/me")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> completeMyProfile(
            @Valid @RequestBody CompleteCitizenProfileRequest request) {
        citizenService.completeProfile(request);
        return ResponseEntity.ok(ApiResponse.of("Profile completed successfully", null));
    }

    /** Update own mutable extra fields (address/ward/zone). */
    @PutMapping("/me")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> updateMyProfile(
            @Valid @RequestBody UpdateCitizenProfileRequest request) {
        citizenService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.of("Profile updated successfully", null));
    }

    // --- Officer-facing (roles FO / DS / ADM) ---

    /** Citizens awaiting verification (status Active). */
    @GetMapping("/pendingVerifications")
    @PreAuthorize("hasAnyRole('FO','DS','ADM')")
    public ResponseEntity<ApiResponse> getPendingVerifications() {
        return ResponseEntity.ok(ApiResponse.data(citizenService.getPendingVerifications()));
    }

    /** Verify or flag a citizen (status V or F). */
    @PutMapping("/{userId}/verify")
    @PreAuthorize("hasAnyRole('FO','DS','ADM')")
    public ResponseEntity<ApiResponse> verifyCitizen(
            @PathVariable String userId,
            @Valid @RequestBody VerifyCitizenRequest request) {
        citizenService.verifyCitizen(userId, request);
        return ResponseEntity.ok(ApiResponse.of("Citizen verification updated successfully", null));
    }

    /** Officer listing of citizens in a ward. */
    @GetMapping("/getCitizensByWard/{ward}")
    @PreAuthorize("hasAnyRole('FO','DS','ADM')")
    public ResponseEntity<ApiResponse> getCitizensByWard(@PathVariable String ward) {
        return ResponseEntity.ok(ApiResponse.data(citizenService.getCitizensByWard(ward)));
    }

    /** Officer listing of every citizen. */
    @GetMapping("/getAllCitizens")
    @PreAuthorize("hasAnyRole('FO','DS','ADM')")
    public ResponseEntity<ApiResponse> getAllCitizens() {
        return ResponseEntity.ok(ApiResponse.data(citizenService.getAllCitizens()));
    }
}
