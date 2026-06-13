package com.civicdesk.module.grievance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicdesk.common.exception.grievance.GrievanceNotFoundException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceDataException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceStateException;
import com.civicdesk.common.exception.grievance.UnauthorizedGrievanceAccessException;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.grievance.dto.request.AssignFieldOfficerReq;
import com.civicdesk.module.grievance.dto.request.ResolveReq;
import com.civicdesk.module.grievance.dto.response.GrievanceDetailResponse;
import com.civicdesk.module.grievance.dto.response.GrievanceSummaryResponse;
import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.enums.ActionType;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;
import com.civicdesk.module.grievance.mapper.GrievanceMapper;
import com.civicdesk.module.grievance.repository.GrievanceActionRepo;
import com.civicdesk.module.grievance.repository.GrievanceRepo;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.UserRepository;

/**
 * Department-supervisor (DS) grievance operations: view the department queue,
 * assign/reassign a field officer, resolve, and view one grievance. A supervisor
 * may act only within their own department. User identity/listing belongs to IAM;
 * here we only read users by id (via {@link UserRepository}) to resolve the
 * supervisor's department and to validate a chosen field officer.
 */
@Service
public class SupervisorGrievanceService {

    private final GrievanceRepo grievanceRepo;
    private final GrievanceActionRepo grievanceActionRepo;
    private final GrievanceMapper mapper;
    private final UserRepository userRepository;

    public SupervisorGrievanceService(GrievanceRepo grievanceRepo,
                                      GrievanceActionRepo grievanceActionRepo,
                                      GrievanceMapper mapper,
                                      UserRepository userRepository) {
        this.grievanceRepo = grievanceRepo;
        this.grievanceActionRepo = grievanceActionRepo;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    /** Grievances in the supervisor's department. */
    @Transactional(readOnly = true)
    public List<GrievanceSummaryResponse> getDepartmentGrievances() {
        String deptId = supervisorDepartmentId();
        return grievanceRepo.findByDepartmentId(deptId)
                .stream().map(mapper::toSummary).toList();
    }

    /** Assign (or reassign) a field officer to a grievance in the supervisor's department. */
    @Transactional
    public GrievanceSummaryResponse assignFieldOfficer(String grievanceId, AssignFieldOfficerReq req) {
        String deptId = supervisorDepartmentId();
        Grievance grievance = loadInDept(grievanceId, deptId);
        requireWithSupervisor(grievance);
        validateFieldOfficer(req.getFieldOfficerId(), deptId);

        grievance.setFieldOfficerId(req.getFieldOfficerId());
        grievance.setAssignedToId(req.getFieldOfficerId());
        grievance.setEscalationLevel(EscalationLevel.L1);
        grievance.setStatus(GrievanceStatus.IP);
        Grievance saved = grievanceRepo.save(grievance);

        logAction(grievanceId, ActionType.AS, "Assigned to field officer", req.getMessage());
        return mapper.toSummary(saved);
    }

    /** Resolve a grievance (with a message); it then goes to the citizen for close/reopen. */
    @Transactional
    public GrievanceSummaryResponse resolveGrievance(String grievanceId, ResolveReq req) {
        String deptId = supervisorDepartmentId();
        Grievance grievance = loadInDept(grievanceId, deptId);
        requireWithSupervisor(grievance);

        grievance.setStatus(GrievanceStatus.R);
        Grievance saved = grievanceRepo.save(grievance);

        logAction(grievanceId, ActionType.RS, "Grievance resolved by supervisor", req.getMessage());
        return mapper.toSummary(saved);
    }

    /** One grievance in the supervisor's department, with its timeline. */
    @Transactional(readOnly = true)
    public GrievanceDetailResponse viewDepartmentGrievance(String grievanceId) {
        String deptId = supervisorDepartmentId();
        Grievance grievance = loadInDept(grievanceId, deptId);
        List<GrievanceAction> actions =
                grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc(grievanceId);
        return mapper.toDetail(grievance, actions);
    }

    // --- helpers ---

    /** The supervisor's department, resolved from their user record. */
    private String supervisorDepartmentId() {
        String userId = SecurityContextUtil.getCurrentUserId();
        User supervisor = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedGrievanceAccessException(
                        "Your account could not be found"));
        String deptId = supervisor.getDepartmentId();
        if (deptId == null || deptId.isBlank()) {
            throw new UnauthorizedGrievanceAccessException(
                    "Your account is not assigned to a department");
        }
        return deptId;
    }

    /** Load a grievance and confirm it belongs to the supervisor's department. */
    private Grievance loadInDept(String grievanceId, String deptId) {
        Grievance grievance = grievanceRepo.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException(
                        "No grievance found with id: " + grievanceId));
        if (!deptId.equals(grievance.getDepartmentId())) {
            throw new UnauthorizedGrievanceAccessException(
                    "This grievance does not belong to your department");
        }
        return grievance;
    }

    /** Assign/resolve are allowed only while the grievance is with the supervisor (L2, not resolved/closed). */
    private void requireWithSupervisor(Grievance grievance) {
        boolean withSupervisor = grievance.getEscalationLevel() == EscalationLevel.L2;
        boolean actionable = grievance.getStatus() != GrievanceStatus.R
                && grievance.getStatus() != GrievanceStatus.C;
        if (!withSupervisor || !actionable) {
            throw new InvalidGrievanceStateException(
                    "This grievance is not currently with the supervisor "
                            + "(it may be with a field officer, resolved, or closed)");
        }
    }

    /** The chosen field officer must be an active FO in the supervisor's department. */
    private void validateFieldOfficer(String fieldOfficerId, String deptId) {
        User officer = userRepository.findById(fieldOfficerId)
                .orElseThrow(() -> new InvalidGrievanceDataException(
                        "No user found with id: " + fieldOfficerId));
        if (!Role.FO.name().equals(officer.getRole())) {
            throw new InvalidGrievanceDataException("The selected user is not a field officer");
        }
        if (!deptId.equals(officer.getDepartmentId())) {
            throw new InvalidGrievanceDataException(
                    "The selected field officer is not in your department");
        }
        if (!UserStatus.ACT.getLabel().equals(officer.getStatus())) {
            throw new InvalidGrievanceDataException("The selected field officer is not active");
        }
    }

    private void logAction(String grievanceId, ActionType type, String title, String message) {
        GrievanceAction action = new GrievanceAction();
        action.setGrievanceId(grievanceId);
        action.setTakenById(SecurityContextUtil.getCurrentUserId());
        action.setActionType(type);
        action.setGrievanceActionTitle(title);
        action.setActionDescription(message);
        grievanceActionRepo.save(action);
    }
}
