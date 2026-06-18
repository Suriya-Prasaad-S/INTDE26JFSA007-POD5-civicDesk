package com.civicdesk.module.citizen.controller;

import com.civicdesk.common.exception.citizen.InvalidRequestException;
import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.util.ClientIpUtil;
import com.civicdesk.module.citizen.dto.request.CitizenRegistrationRequest;
import com.civicdesk.module.citizen.dto.request.UpdateCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.VerifyCitizenRequest;
import com.civicdesk.module.citizen.service.CitizenService;
import com.civicdesk.module.citizen.support.FileStorageService;
import com.civicdesk.module.citizen.support.IdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Citizen profile endpoints under base path {@code /citizenProfile} (served below the application
 * context path {@code /civicDesk}). All responses use the shared {@link ApiResponse} envelope,
 * except the binary proof download.
 *
 * <p>{@code POST /register} is public (creates the User via IAM + the CitizenProfile). The rest are
 * gated by the JWT: {@code /me} requires {@code CIT}; the officer endpoints require
 * {@code FO}/{@code DS}/{@code ADM}.
 */
@RestController
@RequestMapping("/citizenProfile")
public class CitizenController {

    private final CitizenService citizenService;
    private final FileStorageService fileStorage;

    public CitizenController(CitizenService citizenService, FileStorageService fileStorage) {
        this.citizenService = citizenService;
        this.fileStorage = fileStorage;
    }

    // --- Public registration ---

    /**
     * POST /register (multipart/form-data) — citizen self-registration. Form fields are the account
     * + profile data; the {@code proof} part is the identity-proof file. Creates the User (via IAM)
     * and the CitizenProfile together.
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> register(
            @Valid @ModelAttribute CitizenRegistrationRequest request,
            @RequestParam("proof") MultipartFile proof,
            HttpServletRequest http) {
        if (proof == null || proof.isEmpty()) {
            throw new InvalidRequestException("proof file is required");
        }
        String ext = extensionOf(proof.getOriginalFilename());
        String storedName = ext.isEmpty() ? IdGenerator.newId() : IdGenerator.newId() + "." + ext;

        try (InputStream in = proof.getInputStream()) {
            fileStorage.store(in, storedName);
        } catch (IOException e) {
            throw new InvalidRequestException("Could not read the uploaded proof file");
        }

        try {
            citizenService.registerCitizen(request, storedName, ClientIpUtil.resolve(http));
            return ResponseEntity.status(201).body(ApiResponse.of("Citizen registered successfully", null));
        } catch (RuntimeException ex) {
            fileStorage.deleteQuietly(storedName); // roll back the stored proof on failure
            throw ex;
        }
    }

    // --- Citizen-facing (role CIT) ---

    /** View own profile + verification state (created at registration). */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.data(citizenService.getMyProfile()));
    }

    /** Update own mutable fields (address/ward/zone). */
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

    /**
     * GET /{userId}/proof — streams the citizen's identity-proof file. Authorized to the owning
     * citizen or an officer (enforced in the service).
     */
    @GetMapping("/{userId}/proof")
    @PreAuthorize("hasAnyRole('CIT','FO','DS','ADM')")
    public ResponseEntity<Resource> getProof(@PathVariable String userId) {
        String filename = citizenService.resolveProofFileName(userId);
        Resource resource = fileStorage.load(filename);
        return ResponseEntity.ok()
                .contentType(contentTypeFor(extensionOf(filename)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // --- Helpers ---

    /** Lowercased extension without the dot, or "" if none. */
    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private static MediaType contentTypeFor(String ext) {
        return switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
