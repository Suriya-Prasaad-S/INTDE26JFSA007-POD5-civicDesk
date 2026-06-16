package com.civicdesk.module.citizen.service;

import com.civicdesk.module.citizen.dto.request.RegisterCitizenRequest;
import com.civicdesk.module.citizen.dto.request.UpdateCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.UpdateCitizenStatusRequest;
import com.civicdesk.module.citizen.dto.response.CitizenProfileResponse;
import com.civicdesk.module.citizen.dto.response.CitizenSummaryResponse;
import com.civicdesk.module.citizen.entity.CitizenProfile;
import com.civicdesk.module.citizen.entity.enums.CitizenStatus;
import com.civicdesk.module.citizen.entity.enums.Gender;
import com.civicdesk.module.citizen.exception.BusinessRuleException;
import com.civicdesk.module.citizen.exception.DuplicateResourceException;
import com.civicdesk.module.citizen.exception.InvalidRequestException;
import com.civicdesk.module.citizen.exception.ResourceNotFoundException;
import com.civicdesk.module.citizen.repository.CitizenProfileRepository;
import com.civicdesk.module.citizen.support.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for the citizen profile lifecycle.
 *
 * <p>{@code citizenId} is a 16-character alphanumeric id ({@link IdGenerator}). {@code status} is
 * carried as the {@link CitizenStatus} enum and exposed on the API as its single-character code.
 */
@Service
@Transactional(readOnly = true)
public class CitizenService {

    private final CitizenProfileRepository citizenRepository;

    public CitizenService(CitizenProfileRepository citizenRepository) {
        this.citizenRepository = citizenRepository;
    }

    /**
     * Registers a new citizen: rejects a duplicate email / national ID (409), assigns a fresh
     * 16-char {@code citizenId}, and sets the initial status to {@link CitizenStatus#Active}.
     *
     * @return the generated {@code citizenId} (not exposed on the API response, but useful internally)
     */
    @Transactional
    public String registerCitizen(RegisterCitizenRequest request) {
        if (citizenRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        String nationalId = request.nationalIdNumber();
        if (nationalId != null && !nationalId.isBlank()
                && citizenRepository.existsByNationalIdNumber(nationalId)) {
            throw new DuplicateResourceException("National ID already registered: " + nationalId);
        }

        CitizenProfile citizen = new CitizenProfile();
        citizen.setCitizenId(IdGenerator.newId());
        citizen.setName(request.name());
        citizen.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null && !request.gender().isBlank()) {
            citizen.setGender(parseEnum(Gender.class, request.gender(), "gender"));
        }
        citizen.setNationalIdNumber(nationalId);
        citizen.setAddress(request.address());
        citizen.setWard(request.ward());
        citizen.setZone(request.zone());
        citizen.setEmail(request.email());
        citizen.setPhone(request.phone());
        citizen.setStatus(CitizenStatus.Active);

        citizenRepository.save(citizen);
        return citizen.getCitizenId();
    }

    /** Loads a citizen (404 if missing) and returns it with the national ID masked. */
    public CitizenProfileResponse getProfile(String citizenId) {
        CitizenProfile citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found: " + citizenId));
        return toProfileResponse(citizen);
    }

    /**
     * Patches the mutable fields of a profile. Only non-null fields are applied; a request with no
     * updatable fields is rejected (400). Email, gender, date of birth and national ID are not
     * updatable through this endpoint by design.
     */
    @Transactional
    public void updateProfile(String citizenId, UpdateCitizenProfileRequest request) {
        if (isEmptyUpdate(request)) {
            throw new InvalidRequestException("No updatable fields provided");
        }
        CitizenProfile citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found: " + citizenId));

        if (request.name() != null) {
            citizen.setName(request.name());
        }
        if (request.address() != null) {
            citizen.setAddress(request.address());
        }
        if (request.ward() != null) {
            citizen.setWard(request.ward());
        }
        if (request.zone() != null) {
            citizen.setZone(request.zone());
        }
        if (request.phone() != null) {
            citizen.setPhone(request.phone());
        }
        citizenRepository.save(citizen);
    }

    /**
     * Moves a citizen to a new status (supplied as a single-character code), enforcing the allowed
     * transitions: {@code A&rarr;V}, {@code A&harr;F}, {@code V&rarr;F}. {@code V&rarr;A} is not
     * allowed. An unknown code is a 400; an illegal (but well-formed) transition is a 409.
     */
    @Transactional
    public void updateStatus(String citizenId, UpdateCitizenStatusRequest request) {
        CitizenStatus target = parseStatus(request.status());
        CitizenProfile citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found: " + citizenId));

        CitizenStatus current = citizen.getStatus();
        if (!isAllowedTransition(current, target)) {
            throw new BusinessRuleException(
                    "Illegal citizen status transition: " + current.getCode() + " -> " + target.getCode());
        }
        citizen.setStatus(target);
        citizenRepository.save(citizen);
    }

    /** Returns a lightweight summary of every citizen in the given ward. */
    public List<CitizenSummaryResponse> getCitizensByWard(String ward) {
        return citizenRepository.findByWard(ward).stream()
                .map(CitizenService::toSummary)
                .toList();
    }

    /** Returns a lightweight summary of every citizen (optional listing). */
    public List<CitizenSummaryResponse> getAllCitizens() {
        return citizenRepository.findAll().stream()
                .map(CitizenService::toSummary)
                .toList();
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** Allowed citizen status transitions; anything not listed here (incl. same-state) is rejected. */
    private static boolean isAllowedTransition(CitizenStatus from, CitizenStatus to) {
        return switch (from) {
            case Active -> to == CitizenStatus.Verified || to == CitizenStatus.Flagged;
            case Verified -> to == CitizenStatus.Flagged;
            case Flagged -> to == CitizenStatus.Active;
        };
    }

    private static boolean isEmptyUpdate(UpdateCitizenProfileRequest r) {
        return r.name() == null && r.address() == null && r.ward() == null
                && r.zone() == null && r.phone() == null;
    }

    private static CitizenSummaryResponse toSummary(CitizenProfile c) {
        return new CitizenSummaryResponse(
                c.getCitizenId(), c.getName(), c.getWard(), c.getStatus().getCode());
    }

    private CitizenProfileResponse toProfileResponse(CitizenProfile c) {
        return new CitizenProfileResponse(
                c.getCitizenId(),
                c.getName(),
                c.getDateOfBirth(),
                c.getGender() == null ? null : c.getGender().name(),
                maskNationalId(c.getNationalIdNumber()),
                c.getAddress(),
                c.getWard(),
                c.getZone(),
                c.getEmail(),
                c.getPhone(),
                c.getStatus().getCode());
    }

    /**
     * Masks the national ID so the full value is never exposed: keeps only the last 4 characters,
     * e.g. {@code IND1234567890 -> ****7890}. Returns {@code null} for a null/blank value.
     */
    private static String maskNationalId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    /** Parses a single-character status code, raising a precise 400 listing the allowed codes. */
    private static CitizenStatus parseStatus(String value) {
        try {
            return CitizenStatus.fromCode(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidRequestException(
                    "Invalid status: '" + value + "'. Allowed codes: " + CitizenStatus.allowedCodes());
        }
    }

    /** Parses a String into an enum constant, raising a precise 400 listing the allowed values. */
    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            String allowed = Arrays.stream(type.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new InvalidRequestException(
                    "Invalid " + field + ": '" + value + "'. Allowed values: " + allowed);
        }
    }
}
