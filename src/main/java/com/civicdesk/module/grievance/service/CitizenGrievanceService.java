package com.civicdesk.module.grievance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicdesk.common.exception.grievance.GrievanceNotFoundException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceDataException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceStateException;
import com.civicdesk.common.exception.grievance.UnauthorizedGrievanceAccessException;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceDetailsUpdateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceReopenReq;
import com.civicdesk.module.grievance.dto.response.GrievanceDetailResponse;
import com.civicdesk.module.grievance.dto.response.GrievanceResponse;
import com.civicdesk.module.grievance.dto.response.GrievanceSummaryResponse;
import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.enums.ActionType;
import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;
import com.civicdesk.module.grievance.mapper.GrievanceMapper;
import com.civicdesk.module.grievance.repository.GrievanceActionRepo;
import com.civicdesk.module.grievance.repository.GrievanceRepo;
import com.civicdesk.module.iam.entity.Department;
import com.civicdesk.module.iam.repository.DepartmentRepository;

/**
 * Citizen-side grievance operations. The caller's id comes from the JWT (via
 * {@link SecurityContextUtil}); a citizen may act only on their own grievances.
 */
@Service
public class CitizenGrievanceService {

    private final GrievanceRepo grievanceRepo;
    private final GrievanceActionRepo grievanceActionRepo;
    private final DepartmentRepository departmentRepository;
    private final GrievanceMapper mapper;

    public CitizenGrievanceService(GrievanceRepo grievanceRepo,
                                   GrievanceActionRepo grievanceActionRepo,
                                   DepartmentRepository departmentRepository,
                                   GrievanceMapper mapper) {
        this.grievanceRepo = grievanceRepo;
        this.grievanceActionRepo = grievanceActionRepo;
        this.departmentRepository = departmentRepository;
        this.mapper = mapper;
    }

    /** Raise a grievance; routes to the category's department and lands with its supervisor (L2). */
    @Transactional
    public GrievanceResponse createGrievance(GrievanceCreateReq req) {
        String citizenId = currentUserId();
        Category category = parseCategory(req.getCategory());

        Department department = departmentRepository.findByName(category.getDepartmentName())
                .orElseThrow(() -> new InvalidGrievanceDataException(
                        "No department is configured for category " + category));

        Grievance grievance = mapper.toEntity(req, category, citizenId,
                department.getDepartmentId(), department.getDepartmentSupervisorId());

        return mapper.toResponse(grievanceRepo.save(grievance));
    }

    /** Edit title/description; allowed only while the grievance is Open and owned by the caller. */
    @Transactional
    public GrievanceResponse updateGrievanceDetails(String grievanceId, GrievanceDetailsUpdateReq req) {
        Grievance grievance = loadOwned(grievanceId);
        if (grievance.getStatus() != GrievanceStatus.O) {
            throw new InvalidGrievanceStateException("A grievance can be edited only while it is Open");
        }
        grievance.setGrievanceTitle(req.getGrievanceTitle());
        grievance.setDescription(req.getDescription());
        return mapper.toResponse(grievanceRepo.save(grievance));
    }

    /** The caller's own grievances. */
    @Transactional(readOnly = true)
    public List<GrievanceSummaryResponse> getMyGrievances() {
        return grievanceRepo.findByCitizenId(currentUserId())
                .stream().map(mapper::toSummary).toList();
    }

    /** One of the caller's grievances with its full action timeline. */
    @Transactional(readOnly = true)
    public GrievanceDetailResponse getGrievanceById(String grievanceId) {
        Grievance grievance = loadOwned(grievanceId);
        List<GrievanceAction> actions =
                grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc(grievanceId);
        return mapper.toDetail(grievance, actions);
    }

    /** Citizen accepts a resolved grievance -> Closed (terminal). */
    @Transactional
    public GrievanceResponse closeGrievance(String grievanceId) {
        Grievance grievance = loadOwned(grievanceId);
        requireResolved(grievance);
        grievance.setStatus(GrievanceStatus.C);
        Grievance saved = grievanceRepo.save(grievance);
        logAction(grievanceId, ActionType.CL, "Grievance closed by citizen", null);
        return mapper.toResponse(saved);
    }

    /** Citizen rejects a resolved grievance -> Reopened, back to the department supervisor (L2). */
    @Transactional
    public GrievanceResponse reopenGrievance(String grievanceId, GrievanceReopenReq req) {
        Grievance grievance = loadOwned(grievanceId);
        requireResolved(grievance);
        grievance.setStatus(GrievanceStatus.RO);
        grievance.setEscalationLevel(EscalationLevel.L2);
        if (grievance.getDepartmentId() != null) {
            departmentRepository.findById(grievance.getDepartmentId())
                    .ifPresent(d -> grievance.setAssignedToId(d.getDepartmentSupervisorId()));
        }
        Grievance saved = grievanceRepo.save(grievance);
        logAction(grievanceId, ActionType.RP, "Grievance reopened by citizen", req.getReason());
        return mapper.toResponse(saved);
    }

    // --- helpers ---

    private Grievance loadOwned(String grievanceId) {
        Grievance grievance = grievanceRepo.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException(
                        "No grievance found with id: " + grievanceId));
        if (!grievance.getCitizenId().equals(currentUserId())) {
            throw new UnauthorizedGrievanceAccessException("You can only access your own grievances");
        }
        return grievance;
    }

    private void requireResolved(Grievance grievance) {
        if (grievance.getStatus() != GrievanceStatus.R) {
            throw new InvalidGrievanceStateException(
                    "Only a Resolved grievance can be closed or reopened");
        }
    }

    private void logAction(String grievanceId, ActionType type, String title, String description) {
        GrievanceAction action = new GrievanceAction();
        action.setGrievanceId(grievanceId);
        action.setTakenById(currentUserId());
        action.setActionType(type);
        action.setGrievanceActionTitle(title);
        action.setActionDescription(description);
        grievanceActionRepo.save(action);
    }

    private Category parseCategory(String value) {
        try {
            return Category.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidGrievanceDataException(
                    "Invalid category '" + value + "'. Valid codes: RI, WS, SN, SD, CR, OT");
        }
    }

    private String currentUserId() {
        return SecurityContextUtil.getCurrentUserId();
    }
}
