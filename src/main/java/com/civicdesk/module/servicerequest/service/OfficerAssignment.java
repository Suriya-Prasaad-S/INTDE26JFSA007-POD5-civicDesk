package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.UnprocessableEntityException;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.UserRepository;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.repository.ServiceRequestRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Picks the field officer a new request should be assigned to.
 *
 * <p>Selects the least-loaded active officer in the service's department, where load is
 * the number of non-terminal requests currently assigned to that officer. Backed entirely
 * by JPA repositories (no hand-written SQL), so table/column names are checked at compile
 * time against the entities.</p>
 */
@Component
public class OfficerAssignment {

    /** Requests in these states no longer count towards an officer's active workload. */
    private static final List<RequestStatus> TERMINAL =
            List.of(RequestStatus.Completed, RequestStatus.Rejected);

    private final UserRepository userRepository;
    private final ServiceRequestRepository requestRepository;

    public OfficerAssignment(UserRepository userRepository,
                             ServiceRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
    }

    public User findLeastLoadedOfficer(String departmentId) {
        // Officers are IAM users with role FO (Field Officer) and an Active ("A") status.
        List<User> officers = userRepository.findByRoleAndStatusAndDepartmentId(
                Role.FO.name(), UserStatus.ACT.getLabel(), departmentId);

        if (officers.isEmpty()) {
            throw new UnprocessableEntityException(
                    "No active officer is available in department " + departmentId
                            + " to handle this request.");
        }

        // Small per-officer count query; fine at this scale and far clearer than one big SQL.
        return officers.stream()
                .min(Comparator.comparingLong(this::activeWorkload))
                .orElseThrow();
    }

    private long activeWorkload(User officer) {
        return requestRepository.countByAssignedOfficerAndStatusNotIn(officer, TERMINAL);
    }
}
