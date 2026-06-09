package com.civicdesk.module.grievance.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.civicdesk.common.exception.grievance.GrievanceActionCreationException;
import com.civicdesk.common.exception.grievance.GrievanceCreationException;
import com.civicdesk.common.exception.grievance.GrievanceNotFoundException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceDataException;
import com.civicdesk.common.exception.grievance.InvalidUserRoleException;
import com.civicdesk.module.grievance.dto.response.GrievanceDetailResponse;
import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.repository.GrievanceActionRepo;
import com.civicdesk.module.grievance.repository.GrievanceRepo;

@Service
public class GrievanceService {
    @Autowired
    private GrievanceRepo grievanceRepo;
    @Autowired
    private GrievanceActionRepo grievanceActionRepo;

    /**
     * Persists a new grievance after validating that the mandatory fields are
     * present.
     *
     * @param grievance the grievance to create
     * @return the generated grievance id
     * @throws InvalidGrievanceDataException if a required field is missing
     * @throws GrievanceCreationException    if the grievance could not be saved
     */
    public String createGrievance(Grievance grievance) {
        if (grievance == null) {
            throw new InvalidGrievanceDataException("Grievance payload must not be null");
        }
        if (isBlank(grievance.getCitizenId())) {
            throw new InvalidGrievanceDataException("citizenId is required to create a grievance");
        }
        if (isBlank(grievance.getGrievanceTitle())) {
            throw new InvalidGrievanceDataException("grievanceTitle is required to create a grievance");
        }
        if (isBlank(grievance.getCategory())) {
            throw new InvalidGrievanceDataException("category is required to create a grievance");
        }
        if (isBlank(grievance.getDescription())) {
            throw new InvalidGrievanceDataException("description is required to create a grievance");
        }

        try {
            Grievance saved = grievanceRepo.save(grievance);
            return saved.getGrievanceId();
        } catch (DataAccessException ex) {
            throw new GrievanceCreationException("Failed to create grievance", ex);
        }
    }

    /**
     * Persists a new action against an existing grievance after validating the
     * mandatory fields and confirming the parent grievance exists.
     *
     * @param grievanceAction the action to create
     * @return the generated action id
     * @throws InvalidGrievanceDataException    if a required field is missing
     * @throws GrievanceNotFoundException       if the referenced grievance does not exist
     * @throws GrievanceActionCreationException if the action could not be saved
     */
    public String createGrievanceAction(GrievanceAction grievanceAction) {
        if (grievanceAction == null) {
            throw new InvalidGrievanceDataException("Grievance action payload must not be null");
        }
        if (isBlank(grievanceAction.getGrievanceId())) {
            throw new InvalidGrievanceDataException("grievanceId is required to create a grievance action");
        }
        if (isBlank(grievanceAction.getTakenById())) {
            throw new InvalidGrievanceDataException("takenById is required to create a grievance action");
        }
        if (isBlank(grievanceAction.getGrievanceActionTitle())) {
            throw new InvalidGrievanceDataException("grievanceActionTitle is required to create a grievance action");
        }
        if (isBlank(grievanceAction.getActionDescription())) {
            throw new InvalidGrievanceDataException("actionDescription is required to create a grievance action");
        }

        if (!grievanceRepo.existsById(grievanceAction.getGrievanceId())) {
            throw new GrievanceNotFoundException(
                    "No grievance found with id: " + grievanceAction.getGrievanceId());
        }

        try {
            GrievanceAction saved = grievanceActionRepo.save(grievanceAction);
            return saved.getActionId();
        } catch (DataAccessException ex) {
            throw new GrievanceActionCreationException("Failed to create grievance action", ex);
        }
    }

    /**
     * Returns the grievances visible to a user, scoped by their role:
     * <ul>
     *   <li>{@code CITIZEN} – grievances they raised ({@code citizenId})</li>
     *   <li>{@code FIELD_OFFICER} – grievances assigned to them ({@code assignedToId})</li>
     *   <li>{@code DEPARTMENT_SUPERVISOR} / {@code ADMIN} – every grievance</li>
     * </ul>
     * Note: the grievance entity does not yet carry a department/ownership link,
     * so supervisors and admins currently see all grievances. Once that mapping
     * exists this method can be tightened for the supervisor role.
     *
     * @param role   the requesting user's role (case-insensitive)
     * @param userId the requesting user's id; required for CITIZEN and FIELD_OFFICER
     * @return the grievances the user is allowed to see
     * @throws InvalidGrievanceDataException if a required argument is missing
     * @throws InvalidUserRoleException      if the role is not recognised
     */
    public List<Grievance> getGrievancesByRole(String role, String userId) {
        if (isBlank(role)) {
            throw new InvalidGrievanceDataException("role is required to fetch grievances");
        }

        String normalizedRole = role.trim().toUpperCase();
        switch (normalizedRole) {
            case "CITIZEN":
                requireUserId(userId, normalizedRole);
                return grievanceRepo.findByCitizenId(userId);
            case "FIELD_OFFICER":
                requireUserId(userId, normalizedRole);
                return grievanceRepo.findByAssignedToId(userId);
            case "DEPARTMENT_SUPERVISOR":
            case "ADMIN":
                return grievanceRepo.findAll();
            default:
                throw new InvalidUserRoleException("Unsupported role '" + role
                        + "'. Valid roles are: CITIZEN, FIELD_OFFICER, DEPARTMENT_SUPERVISOR, ADMIN");
        }
    }

    /**
     * Returns a single grievance together with all actions recorded against it,
     * ordered oldest first.
     *
     * @param grievanceId the grievance to load
     * @return the grievance and its related actions
     * @throws InvalidGrievanceDataException if {@code grievanceId} is missing
     * @throws GrievanceNotFoundException    if no grievance exists with that id
     */
    public GrievanceDetailResponse getGrievanceDetails(String grievanceId) {
        if (isBlank(grievanceId)) {
            throw new InvalidGrievanceDataException("grievanceId is required to fetch grievance details");
        }

        Grievance grievance = grievanceRepo.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException(
                        "No grievance found with id: " + grievanceId));

        List<GrievanceAction> actions =
                grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc(grievanceId);

        return GrievanceDetailResponse.builder()
                .grievance(grievance)
                .actions(actions)
                .build();
    }

    private void requireUserId(String userId, String role) {
        if (isBlank(userId)) {
            throw new InvalidGrievanceDataException("userId is required when role is " + role);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
