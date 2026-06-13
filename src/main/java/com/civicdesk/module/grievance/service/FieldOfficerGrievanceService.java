package com.civicdesk.module.grievance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicdesk.common.exception.grievance.ActionNotEditableException;
import com.civicdesk.common.exception.grievance.ActionNotFoundException;
import com.civicdesk.common.exception.grievance.GrievanceNotFoundException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceDataException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceStateException;
import com.civicdesk.common.exception.grievance.UnauthorizedGrievanceAccessException;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.grievance.dto.request.GrievanceActionCreateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceActionUpdateReq;
import com.civicdesk.module.grievance.dto.response.GrievanceActionResponse;
import com.civicdesk.module.grievance.dto.response.GrievanceDetailResponse;
import com.civicdesk.module.grievance.dto.response.GrievanceSummaryResponse;
import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.enums.ActionStatus;
import com.civicdesk.module.grievance.enums.ActionType;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;
import com.civicdesk.module.grievance.mapper.GrievanceMapper;
import com.civicdesk.module.grievance.repository.GrievanceActionRepo;
import com.civicdesk.module.grievance.repository.GrievanceRepo;
import com.civicdesk.module.iam.entity.Department;
import com.civicdesk.module.iam.repository.DepartmentRepository;

/**
 * Field-officer grievance operations (also used by a supervisor doing the work directly in a
 * department with no field officer). The caller acts only on grievances assigned to them.
 */
@Service
public class FieldOfficerGrievanceService {

    private final GrievanceRepo grievanceRepo;
    private final GrievanceActionRepo grievanceActionRepo;
    private final DepartmentRepository departmentRepository;
    private final GrievanceMapper mapper;

    public FieldOfficerGrievanceService(GrievanceRepo grievanceRepo,
                                        GrievanceActionRepo grievanceActionRepo,
                                        DepartmentRepository departmentRepository,
                                        GrievanceMapper mapper) {
        this.grievanceRepo = grievanceRepo;
        this.grievanceActionRepo = grievanceActionRepo;
        this.departmentRepository = departmentRepository;
        this.mapper = mapper;
    }

    /** Grievances currently assigned to the caller. */
    @Transactional(readOnly = true)
    public List<GrievanceSummaryResponse> getAssignedGrievances() {
        return grievanceRepo.findByAssignedToId(me()).stream().map(mapper::toSummary).toList();
    }

    /** One of the caller's assigned grievances with its full action timeline. */
    @Transactional(readOnly = true)
    public GrievanceDetailResponse viewAssignedGrievance(String grievanceId) {
        Grievance grievance = loadAssigned(grievanceId);
        return mapper.toDetail(grievance,
                grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc(grievanceId));
    }

    /** Create a WORK action on an assigned grievance (status starts Open). */
    @Transactional
    public GrievanceActionResponse createGrievanceAction(String grievanceId, GrievanceActionCreateReq req) {
        Grievance grievance = loadAssigned(grievanceId);
        requireWorkable(grievance);

        boolean openWork = grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc(grievanceId).stream()
                .anyMatch(a -> a.getActionType() == ActionType.WK && a.getStatus() != ActionStatus.CM);
        if (openWork) {
            throw new InvalidGrievanceStateException(
                    "An open work action already exists; complete it before creating another");
        }

        GrievanceAction action = new GrievanceAction();
        action.setGrievanceId(grievanceId);
        action.setTakenById(me());
        action.setActionType(ActionType.WK);
        action.setGrievanceActionTitle(req.getGrievanceActionTitle());
        action.setActionDescription(req.getActionDescription());
        action.setStatus(ActionStatus.O);
        return mapper.toActionResponse(grievanceActionRepo.save(action));
    }

    /**
     * Update a WORK action's status/content. Setting status to {@code CM} (Completed) hands
     * the grievance to the department supervisor for review (when a field officer did the
     * work). A supervisor working a no-FO department's grievance just finalizes the action.
     */
    @Transactional
    public GrievanceActionResponse updateGrievanceAction(String actionId, GrievanceActionUpdateReq req) {
        GrievanceAction action = grievanceActionRepo.findById(actionId)
                .orElseThrow(() -> new ActionNotFoundException("No action found with id: " + actionId));

        Grievance grievance = loadAssigned(action.getGrievanceId());
        requireWorkable(grievance);
        requireEditable(action);

        ActionStatus newStatus = parseStatus(req.getStatus());

        if (req.getGrievanceActionTitle() != null && !req.getGrievanceActionTitle().isBlank()) {
            action.setGrievanceActionTitle(req.getGrievanceActionTitle());
        }
        if (req.getActionDescription() != null && !req.getActionDescription().isBlank()) {
            action.setActionDescription(req.getActionDescription());
        }
        action.setStatus(newStatus);
        GrievanceAction saved = grievanceActionRepo.save(action);

        // Completed by a field officer (grievance at L1) -> send up to the supervisor for review.
        if (newStatus == ActionStatus.CM && grievance.getEscalationLevel() == EscalationLevel.L1) {
            appendReview(grievance);
            grievance.setEscalationLevel(EscalationLevel.L2);
            grievance.setAssignedToId(resolveSupervisor(grievance.getDepartmentId()));
            grievanceRepo.save(grievance);
        }
        // If already at L2 (a supervisor doing the work in a no-FO dept), completing just
        // finalizes the action; the supervisor then resolves it directly.

        return mapper.toActionResponse(saved);
    }

    // --- helpers ---

    private Grievance loadAssigned(String grievanceId) {
        Grievance grievance = grievanceRepo.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException(
                        "No grievance found with id: " + grievanceId));
        if (!me().equals(grievance.getAssignedToId())) {
            throw new UnauthorizedGrievanceAccessException("This grievance is not assigned to you");
        }
        return grievance;
    }

    private void requireWorkable(Grievance grievance) {
        GrievanceStatus s = grievance.getStatus();
        if (s == GrievanceStatus.C || s == GrievanceStatus.R) {
            throw new InvalidGrievanceStateException(
                    "Cannot work a grievance that is Resolved or Closed");
        }
    }

    private void requireEditable(GrievanceAction action) {
        if (action.getActionType() != ActionType.WK) {
            throw new ActionNotEditableException("Only work actions can be edited");
        }
        if (!me().equals(action.getTakenById())) {
            throw new ActionNotEditableException("Only the creator can edit this action");
        }
        if (action.getStatus() == ActionStatus.CM) {
            throw new ActionNotEditableException("A completed action can no longer be edited");
        }
        List<GrievanceAction> actions =
                grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc(action.getGrievanceId());
        GrievanceAction latest = actions.get(actions.size() - 1);
        if (!latest.getActionId().equals(action.getActionId())) {
            throw new ActionNotEditableException("Only the latest action can be edited");
        }
    }

    private void appendReview(Grievance grievance) {
        GrievanceAction review = new GrievanceAction();
        review.setGrievanceId(grievance.getGrievanceId());
        review.setTakenById(me());
        review.setActionType(ActionType.RV);
        review.setGrievanceActionTitle("Sent to supervisor for review");
        grievanceActionRepo.save(review);
    }

    private String resolveSupervisor(String departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .map(Department::getDepartmentSupervisorId)
                .orElse(null);
    }

    private ActionStatus parseStatus(String value) {
        try {
            return ActionStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidGrievanceDataException(
                    "Invalid status '" + value + "'. Valid codes: O, IP, CM");
        }
    }

    private String me() {
        return SecurityContextUtil.getCurrentUserId();
    }
}
