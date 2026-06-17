package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.ForbiddenException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.UserRepository;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.repository.CitizenProfileRepository;
import org.springframework.stereotype.Component;

/**
 * Read-only access to citizen data, joining the Service Request module's
 * {@code citizen_profile} to the IAM-owned {@code users} account.
 *
 * <p>Per the ER design a citizen's account status (Active / Inactive / Suspended) lives on
 * the {@code users} row linked from {@code citizen_profile.userId}. Only an Active ("A")
 * account may submit requests.</p>
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
     * @throws ForbiddenException        if the citizen's linked account is not Active
     */
    public CitizenProfile loadSubmittableCitizen(String citizenId) {
        CitizenProfile citizen = citizenProfileRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Citizen with ID " + citizenId + " does not exist"));

        User account = userRepository.findById(citizen.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User account for citizen " + citizenId + " does not exist"));

        // Only an Active ("A") account may submit; Inactive/Suspended (or any flagged) accounts are blocked.
        if (!UserStatus.ACT.getLabel().equalsIgnoreCase(account.getStatus())) {
            throw new ForbiddenException(
                    "Your citizen account is not active (it may be flagged or suspended). "
                            + "Please contact your ward office.");
        }
        return citizen;
    }
}
