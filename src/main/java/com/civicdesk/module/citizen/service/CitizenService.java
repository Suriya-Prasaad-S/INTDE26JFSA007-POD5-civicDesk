package com.civicdesk.module.citizen.service;

import com.civicdesk.common.util.NationalIdUtil;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.citizen.dto.request.CompleteCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.UpdateCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.VerifyCitizenRequest;
import com.civicdesk.module.citizen.dto.response.CitizenProfileResponse;
import com.civicdesk.module.citizen.dto.response.CitizenSummaryResponse;
import com.civicdesk.module.citizen.entity.CitizenProfile;
import com.civicdesk.module.citizen.entity.enums.CitizenStatus;
import com.civicdesk.module.citizen.entity.enums.Gender;
import com.civicdesk.common.exception.citizen.BusinessRuleException;
import com.civicdesk.common.exception.citizen.DuplicateResourceException;
import com.civicdesk.common.exception.citizen.InvalidRequestException;
import com.civicdesk.common.exception.citizen.ResourceNotFoundException;
import com.civicdesk.module.citizen.repository.CitizenProfileRepository;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business logic for the citizen profile lifecycle.
 *
 * <p>A {@link CitizenProfile} shares its primary key with the IAM {@code User} ({@code userId}).
 * Identity fields (name/email/phone) are read from {@code User} via {@link UserRepository}; this
 * service owns only the citizen-specific extras and the verification {@code status}.
 *
 * <p>Lifecycle: a stub profile (status {@link CitizenStatus#Active}) is auto-created on the
 * citizen's first {@code GET /me}; an officer then verifies them ({@code Active -> Verified}); the
 * verified citizen completes the extra-fields form.
 */
@Service
@Transactional(readOnly = true)
public class CitizenService {

    private final CitizenProfileRepository citizenRepository;
    private final UserRepository userRepository;

    public CitizenService(CitizenProfileRepository citizenRepository, UserRepository userRepository) {
        this.citizenRepository = citizenRepository;
        this.userRepository = userRepository;
    }

    // ---------------------------------------------------------------------------------------------
    // Citizen-facing (identity from the JWT)
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the current citizen's profile, lazily creating a stub (status {@code Active}) on the
     * first call so the citizen enters the pending-verification queue.
     */
    @Transactional
    public CitizenProfileResponse getMyProfile() {
        String userId = currentUserId();
        CitizenProfile profile = citizenRepository.findById(userId)
                .orElseGet(() -> createStub(userId));
        return toProfileResponse(profile, userRepository.findById(userId).orElse(null));
    }

    /**
     * Completes the current citizen's profile (the extra fields). The citizen must already be
     * {@link CitizenStatus#Verified} (409 otherwise) and the national id must be unique (409).
     */
    @Transactional
    public void completeProfile(CompleteCitizenProfileRequest request) {
        String userId = currentUserId();
        CitizenProfile profile = citizenRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "You must be verified before completing your profile"));

        if (profile.getStatus() != CitizenStatus.Verified) {
            throw new BusinessRuleException(
                    "Profile can be completed only after verification (current status: "
                            + profile.getStatus().getCode() + ")");
        }
        String nationalIdHash = NationalIdUtil.hash(request.nationalIdNumber());
        if (citizenRepository.existsByNationalIdHash(nationalIdHash)) {
            throw new DuplicateResourceException("National ID already registered");
        }

        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(parseGender(request.gender()));
        profile.setNationalIdHash(nationalIdHash);
        profile.setNationalIdLast4(last4(request.nationalIdNumber()));
        profile.setAddress(request.address());
        profile.setWard(request.ward());
        profile.setZone(request.zone());
        citizenRepository.save(profile);
    }

    /** Updates the current citizen's mutable extra fields (address/ward/zone). */
    @Transactional
    public void updateMyProfile(UpdateCitizenProfileRequest request) {
        if (isEmptyUpdate(request)) {
            throw new InvalidRequestException("No updatable fields provided");
        }
        String userId = currentUserId();
        CitizenProfile profile = citizenRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen profile not found"));

        if (profile.getStatus() != CitizenStatus.Verified) {
            throw new BusinessRuleException(
                    "Profile can be updated only after verification (current status: "
                            + profile.getStatus().getCode() + ")");
        }

        if (request.address() != null) {
            profile.setAddress(request.address());
        }
        if (request.ward() != null) {
            profile.setWard(request.ward());
        }
        if (request.zone() != null) {
            profile.setZone(request.zone());
        }
        citizenRepository.save(profile);
    }

    // ---------------------------------------------------------------------------------------------
    // Officer-facing
    // ---------------------------------------------------------------------------------------------

    /** Citizens awaiting verification (status {@code Active}). */
    public List<CitizenSummaryResponse> getPendingVerifications() {
        return toSummaries(citizenRepository.findByStatus(CitizenStatus.Active));
    }

    /**
     * Verifies (or flags) a citizen. {@code status} must be {@code V} or {@code F}; the transition
     * must be allowed (409 otherwise). Stamps the verifying officer and timestamp.
     *
     * <p>KNOWN LIMITATION (revisit later): a {@code Flagged} citizen cannot be reactivated through
     * this endpoint — {@code A} (Active) is rejected as a verify target, so {@code F -> A} has no
     * path here. Reactivating a flagged citizen would need a separate "reactivate" operation.
     */
    @Transactional
    public void verifyCitizen(String citizenUserId, VerifyCitizenRequest request) {
        CitizenStatus target = parseVerifyTarget(request.status());
        CitizenProfile profile = citizenRepository.findById(citizenUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Citizen profile not found: " + citizenUserId));

        if (!isAllowedTransition(profile.getStatus(), target)) {
            throw new BusinessRuleException(
                    "Illegal citizen status transition: " + profile.getStatus().getCode()
                            + " -> " + target.getCode());
        }
        profile.setStatus(target);
        profile.setVerifiedBy(currentUserId());
        profile.setVerifiedAt(LocalDateTime.now());
        citizenRepository.save(profile);
    }

    /** Lightweight summary of every citizen in the given ward. */
    public List<CitizenSummaryResponse> getCitizensByWard(String ward) {
        return toSummaries(citizenRepository.findByWard(ward));
    }

    /** Lightweight summary of every citizen. */
    public List<CitizenSummaryResponse> getAllCitizens() {
        return toSummaries(citizenRepository.findAll());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private CitizenProfile createStub(String userId) {
        CitizenProfile profile = new CitizenProfile();
        profile.setUserId(userId);
        profile.setStatus(CitizenStatus.Active);
        profile.setCreatedBy(userId);
        return citizenRepository.save(profile);
    }

    /** Allowed citizen status transitions (officer-driven). */
    private static boolean isAllowedTransition(CitizenStatus from, CitizenStatus to) {
        return switch (from) {
            case Active -> to == CitizenStatus.Verified || to == CitizenStatus.Flagged;
            case Verified -> to == CitizenStatus.Flagged;
            case Flagged -> to == CitizenStatus.Active;
        };
    }

    private static boolean isEmptyUpdate(UpdateCitizenProfileRequest r) {
        return r.address() == null && r.ward() == null && r.zone() == null;
    }

    /** Builds summaries, sourcing each citizen's name from {@code User} in a single batch query. */
    private List<CitizenSummaryResponse> toSummaries(List<CitizenProfile> profiles) {
        List<String> ids = profiles.stream().map(CitizenProfile::getUserId).toList();
        Map<String, User> users = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        return profiles.stream()
                .map(p -> new CitizenSummaryResponse(
                        p.getUserId(),
                        nameOf(users.get(p.getUserId())),
                        p.getWard(),
                        p.getStatus().getCode()))
                .toList();
    }

    private CitizenProfileResponse toProfileResponse(CitizenProfile p, User user) {
        return new CitizenProfileResponse(
                p.getUserId(),
                nameOf(user),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getPhone(),
                p.getDateOfBirth(),
                p.getGender() == null ? null : p.getGender().name(),
                maskNationalId(p.getNationalIdLast4()),
                p.getAddress(),
                p.getWard(),
                p.getZone(),
                p.getStatus().getCode(),
                p.getVerifiedBy(),
                p.getVerifiedAt(),
                p.getCreatedAt());
    }

    private static String nameOf(User user) {
        return user == null ? null : user.getName();
    }

    /** The last 4 characters of the national id (or the whole value if shorter). */
    private static String last4(String raw) {
        String trimmed = raw.trim();
        return trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
    }

    /** Builds the masked national id for display from the stored last-4 digits ({@code ****1234}). */
    private static String maskNationalId(String last4) {
        return (last4 == null || last4.isBlank()) ? null : "****" + last4;
    }

    /** Case-insensitive parse of the gender name (Male/Female/Other). */
    private static Gender parseGender(String value) {
        for (Gender g : Gender.values()) {
            if (g.name().equalsIgnoreCase(value.trim())) {
                return g;
            }
        }
        throw new InvalidRequestException(
                "Invalid gender: '" + value + "'. Allowed values: Male, Female, Other");
    }

    /** Parses the verify target: must be {@code V} (Verified) or {@code F} (Flagged). */
    private static CitizenStatus parseVerifyTarget(String value) {
        CitizenStatus status;
        try {
            status = CitizenStatus.fromCode(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidRequestException(
                    "Invalid status: '" + value + "'. Allowed verify codes: V, F");
        }
        if (status == CitizenStatus.Active) {
            throw new InvalidRequestException(
                    "'A' (Active) is not a valid verify target; use V (Verified) or F (Flagged)");
        }
        return status;
    }

    private static String currentUserId() {
        String userId = SecurityContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new InvalidRequestException("No authenticated user in the security context");
        }
        return userId;
    }
}
