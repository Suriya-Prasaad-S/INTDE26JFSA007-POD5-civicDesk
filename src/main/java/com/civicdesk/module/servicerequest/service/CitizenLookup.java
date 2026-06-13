package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.ForbiddenException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.entity.external.User;
import com.civicdesk.module.serviceRequest.repository.CitizenProfileRepository;
import com.civicdesk.module.serviceRequest.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Read-only access to citizen data owned by the Citizen / IAM modules.
 *
 * <p>Per the ER design a citizen's account status (Active / Flagged) lives on the
 * {@code users} row linked from {@code citizen_profile.userId}. Backed by JPA repositories
 * over the placeholder entities until those modules land.</p>
 */
@Component
public class CitizenLookup {

    private final CitizenProfileRepository citizenProfileRepository;
    private final UserRepository userRepository;

    public CitizenLookup(CitizenProfileRepository citizenProfileRepository,
                         UserRepository userRepository) {
        this.citizenProfileRepository = citizenProfileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Ensures the citizen exists and is allowed to submit requests, returning the profile.
     *
     * @throws ResourceNotFoundException if no citizen profile matches the id
     * @throws ForbiddenException        if the citizen's linked account is Flagged
     */
    public CitizenProfile loadSubmittableCitizen(String citizenId) {
        CitizenProfile citizen = citizenProfileRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Citizen with ID " + citizenId + " does not exist"));

        User account = userRepository.findById(citizen.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User account for citizen " + citizenId + " does not exist"));

        // User status uses single-letter codes: "F" = Flagged (see DummyDataSeeder).
        if ("F".equalsIgnoreCase(account.getStatus())) {
            throw new ForbiddenException(
                    "Your citizen profile is flagged. Please contact your ward office.");
        }
        return citizen;
    }
}
