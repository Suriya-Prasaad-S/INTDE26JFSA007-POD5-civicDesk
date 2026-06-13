package com.civicdesk.module.grievance.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.civicdesk.module.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.module.grievance.dto.response.GrievanceActionResponse;
import com.civicdesk.module.grievance.dto.response.GrievanceDetailResponse;
import com.civicdesk.module.grievance.dto.response.GrievanceResponse;
import com.civicdesk.module.grievance.dto.response.GrievanceSummaryResponse;
import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;

/** Translates between grievance entities and the citizen-facing DTOs. */
@Component
public class GrievanceMapper {

    /**
     * Builds a new grievance from a create request plus the resolved category,
     * owner and department. Starts at the supervisor tier (L2) and {@code Open}.
     */
    public Grievance toEntity(GrievanceCreateReq req, Category category, String citizenId,
                              String departmentId, String supervisorId) {
        Grievance g = new Grievance();
        g.setCitizenId(citizenId);
        g.setDepartmentId(departmentId);
        g.setAssignedToId(supervisorId);
        g.setFieldOfficerId(null);
        g.setGrievanceTitle(req.getGrievanceTitle());
        g.setCategory(category);
        g.setDescription(req.getDescription());
        g.setWard(req.getWard());
        g.setEscalationLevel(EscalationLevel.L2);
        g.setStatus(GrievanceStatus.O);
        return g;
    }

    public GrievanceResponse toResponse(Grievance g) {
        return GrievanceResponse.builder()
                .grievanceId(g.getGrievanceId())
                .category(g.getCategory())
                .grievanceTitle(g.getGrievanceTitle())
                .description(g.getDescription())
                .ward(g.getWard())
                .status(g.getStatus())
                .escalationLevel(g.getEscalationLevel())
                .submissionDate(g.getSubmissionDate())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    public GrievanceSummaryResponse toSummary(Grievance g) {
        return GrievanceSummaryResponse.builder()
                .grievanceId(g.getGrievanceId())
                .grievanceTitle(g.getGrievanceTitle())
                .category(g.getCategory())
                .status(g.getStatus())
                .escalationLevel(g.getEscalationLevel())
                .submissionDate(g.getSubmissionDate())
                .build();
    }

    public GrievanceActionResponse toActionResponse(GrievanceAction a) {
        return GrievanceActionResponse.builder()
                .actionId(a.getActionId())
                .actionType(a.getActionType())
                .grievanceActionTitle(a.getGrievanceActionTitle())
                .actionDescription(a.getActionDescription())
                .status(a.getStatus())
                .actionDate(a.getActionDate())
                .takenById(a.getTakenById())
                .build();
    }

    public GrievanceDetailResponse toDetail(Grievance g, List<GrievanceAction> actions) {
        return GrievanceDetailResponse.builder()
                .grievance(toResponse(g))
                .actions(actions.stream().map(this::toActionResponse).toList())
                .build();
    }
}
