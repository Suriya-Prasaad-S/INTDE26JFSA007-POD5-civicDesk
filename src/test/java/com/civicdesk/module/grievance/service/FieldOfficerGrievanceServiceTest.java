package com.civicdesk.module.grievance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.civicdesk.common.exception.grievance.ActionNotEditableException;
import com.civicdesk.common.exception.grievance.ActionNotFoundException;
import com.civicdesk.common.exception.grievance.GrievanceNotFoundException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceDataException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceStateException;
import com.civicdesk.common.exception.grievance.UnauthorizedGrievanceAccessException;
import com.civicdesk.module.grievance.dto.request.GrievanceActionCreateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceActionUpdateReq;
import com.civicdesk.module.grievance.dto.response.GrievanceActionResponse;
import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.enums.ActionStatus;
import com.civicdesk.module.grievance.enums.ActionType;
import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;
import com.civicdesk.module.grievance.mapper.GrievanceMapper;
import com.civicdesk.module.grievance.repository.GrievanceActionRepo;
import com.civicdesk.module.grievance.repository.GrievanceRepo;
import com.civicdesk.module.iam.entity.Department;
import com.civicdesk.module.iam.repository.DepartmentRepository;

/** Unit tests for the field-officer {@link FieldOfficerGrievanceService}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FieldOfficerGrievanceServiceTest {

    private static final String FO = "fo-1";
    private static final String DEPT = "dep-1";

    @Mock GrievanceRepo grievanceRepo;
    @Mock GrievanceActionRepo grievanceActionRepo;
    @Mock DepartmentRepository departmentRepository;
    GrievanceMapper mapper = new GrievanceMapper();

    FieldOfficerGrievanceService service;

    @BeforeEach
    void setup() {
        service = new FieldOfficerGrievanceService(grievanceRepo, grievanceActionRepo, departmentRepository, mapper);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(FO, null,
                        List.of(new SimpleGrantedAuthority("ROLE_FO"))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // --- list / view ---

    @Test
    void getAssignedGrievances_returnsList() {
        when(grievanceRepo.findByAssignedToId(FO))
                .thenReturn(List.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        assertThat(service.getAssignedGrievances()).hasSize(1);
    }

    @Test
    void viewAssignedGrievance_returnsDetail() {
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1")).thenReturn(List.of());
        assertThat(service.viewAssignedGrievance("g1").getGrievance()).isNotNull();
    }

    @Test
    void viewAssignedGrievance_notAssignedToMe_throwsForbidden() {
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, "other")));
        assertThatThrownBy(() -> service.viewAssignedGrievance("g1"))
                .isInstanceOf(UnauthorizedGrievanceAccessException.class);
    }

    // --- create work action ---

    @Test
    void createWork_createsOpenAction() {
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1")).thenReturn(List.of());
        when(grievanceActionRepo.save(any(GrievanceAction.class))).thenAnswer(i -> i.getArgument(0));

        GrievanceActionResponse res = service.createGrievanceAction("g1", createReq());

        assertThat(res.getActionType()).isEqualTo(ActionType.WK);
        assertThat(res.getStatus()).isEqualTo(ActionStatus.O);
        verify(grievanceActionRepo).save(any(GrievanceAction.class));
    }

    @Test
    void createWork_openWorkAlreadyExists_throwsConflict() {
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1"))
                .thenReturn(List.of(work("a1", ActionStatus.IP)));
        assertThatThrownBy(() -> service.createGrievanceAction("g1", createReq()))
                .isInstanceOf(InvalidGrievanceStateException.class);
    }

    @Test
    void createWork_notAssignedToMe_throwsForbidden() {
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, "other")));
        assertThatThrownBy(() -> service.createGrievanceAction("g1", createReq()))
                .isInstanceOf(UnauthorizedGrievanceAccessException.class);
    }

    @Test
    void createWork_grievanceResolved_throwsConflict() {
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.R, EscalationLevel.L2, FO)));
        assertThatThrownBy(() -> service.createGrievanceAction("g1", createReq()))
                .isInstanceOf(InvalidGrievanceStateException.class);
    }

    @Test
    void createWork_grievanceNotFound_throwsNotFound() {
        when(grievanceRepo.findById("g1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createGrievanceAction("g1", createReq()))
                .isInstanceOf(GrievanceNotFoundException.class);
    }

    // --- update work action ---

    @Test
    void updateWork_toInProgress_updates_noHandoff() {
        GrievanceAction a = work("a1", ActionStatus.O);
        when(grievanceActionRepo.findById("a1")).thenReturn(Optional.of(a));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1")).thenReturn(List.of(a));
        when(grievanceActionRepo.save(any(GrievanceAction.class))).thenAnswer(i -> i.getArgument(0));

        GrievanceActionResponse res = service.updateGrievanceAction("a1", updateReq("IP"));

        assertThat(res.getStatus()).isEqualTo(ActionStatus.IP);
        verify(grievanceRepo, never()).save(any(Grievance.class));
        verify(grievanceActionRepo, times(1)).save(any(GrievanceAction.class));
    }

    @Test
    void updateWork_completedByFieldOfficer_handsOffToSupervisor() {
        GrievanceAction a = work("a1", ActionStatus.IP);
        Grievance g = assigned(GrievanceStatus.IP, EscalationLevel.L1, FO);
        when(grievanceActionRepo.findById("a1")).thenReturn(Optional.of(a));
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(g));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1")).thenReturn(List.of(a));
        when(grievanceActionRepo.save(any(GrievanceAction.class))).thenAnswer(i -> i.getArgument(0));
        when(grievanceRepo.save(any(Grievance.class))).thenAnswer(i -> i.getArgument(0));
        when(departmentRepository.findById(DEPT)).thenReturn(Optional.of(department()));

        GrievanceActionResponse res = service.updateGrievanceAction("a1", updateReq("CM"));

        assertThat(res.getStatus()).isEqualTo(ActionStatus.CM);
        assertThat(g.getEscalationLevel()).isEqualTo(EscalationLevel.L2);
        assertThat(g.getAssignedToId()).isEqualTo("sup-1");
        verify(grievanceActionRepo, times(2)).save(any(GrievanceAction.class)); // WORK + REVIEW
        verify(grievanceRepo).save(any(Grievance.class));
    }

    @Test
    void updateWork_completedBySupervisorNoFoDept_finalizesOnly() {
        GrievanceAction a = work("a1", ActionStatus.IP);
        Grievance g = assigned(GrievanceStatus.IP, EscalationLevel.L2, FO); // already supervisor tier
        when(grievanceActionRepo.findById("a1")).thenReturn(Optional.of(a));
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(g));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1")).thenReturn(List.of(a));
        when(grievanceActionRepo.save(any(GrievanceAction.class))).thenAnswer(i -> i.getArgument(0));

        GrievanceActionResponse res = service.updateGrievanceAction("a1", updateReq("CM"));

        assertThat(res.getStatus()).isEqualTo(ActionStatus.CM);
        verify(grievanceRepo, never()).save(any(Grievance.class));           // no hand-off
        verify(grievanceActionRepo, times(1)).save(any(GrievanceAction.class)); // only WORK, no REVIEW
    }

    @Test
    void updateWork_notCreator_throwsNotEditable() {
        GrievanceAction a = work("a1", ActionStatus.O);
        a.setTakenById("someone-else");
        when(grievanceActionRepo.findById("a1")).thenReturn(Optional.of(a));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        assertThatThrownBy(() -> service.updateGrievanceAction("a1", updateReq("IP")))
                .isInstanceOf(ActionNotEditableException.class);
    }

    @Test
    void updateWork_alreadyCompleted_throwsNotEditable() {
        GrievanceAction a = work("a1", ActionStatus.CM);
        when(grievanceActionRepo.findById("a1")).thenReturn(Optional.of(a));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        assertThatThrownBy(() -> service.updateGrievanceAction("a1", updateReq("IP")))
                .isInstanceOf(ActionNotEditableException.class);
    }

    @Test
    void updateWork_notLatestAction_throwsNotEditable() {
        GrievanceAction a = work("a1", ActionStatus.O);
        GrievanceAction newer = new GrievanceAction();
        newer.setActionId("a2");
        newer.setGrievanceId("g1");
        newer.setActionType(ActionType.RV);
        when(grievanceActionRepo.findById("a1")).thenReturn(Optional.of(a));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1")).thenReturn(List.of(a, newer));
        assertThatThrownBy(() -> service.updateGrievanceAction("a1", updateReq("IP")))
                .isInstanceOf(ActionNotEditableException.class);
    }

    @Test
    void updateWork_notWorkAction_throwsNotEditable() {
        GrievanceAction a = work("a1", ActionStatus.O);
        a.setActionType(ActionType.AS);
        when(grievanceActionRepo.findById("a1")).thenReturn(Optional.of(a));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        assertThatThrownBy(() -> service.updateGrievanceAction("a1", updateReq("IP")))
                .isInstanceOf(ActionNotEditableException.class);
    }

    @Test
    void updateWork_invalidStatus_throwsBadRequest() {
        GrievanceAction a = work("a1", ActionStatus.O);
        when(grievanceActionRepo.findById("a1")).thenReturn(Optional.of(a));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(assigned(GrievanceStatus.IP, EscalationLevel.L1, FO)));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1")).thenReturn(List.of(a));
        assertThatThrownBy(() -> service.updateGrievanceAction("a1", updateReq("XX")))
                .isInstanceOf(InvalidGrievanceDataException.class);
    }

    @Test
    void updateWork_actionNotFound_throwsNotFound() {
        when(grievanceActionRepo.findById("a1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateGrievanceAction("a1", updateReq("IP")))
                .isInstanceOf(ActionNotFoundException.class);
    }

    // --- fixtures ---

    private GrievanceActionCreateReq createReq() {
        GrievanceActionCreateReq req = new GrievanceActionCreateReq();
        req.setGrievanceActionTitle("Site visit");
        req.setActionDescription("Inspected the location");
        return req;
    }

    private GrievanceActionUpdateReq updateReq(String status) {
        GrievanceActionUpdateReq req = new GrievanceActionUpdateReq();
        req.setStatus(status);
        return req;
    }

    private Grievance assigned(GrievanceStatus status, EscalationLevel level, String assignedTo) {
        Grievance g = new Grievance();
        g.setGrievanceId("g1");
        g.setCitizenId("c1");
        g.setDepartmentId(DEPT);
        g.setCategory(Category.RI);
        g.setGrievanceTitle("Title");
        g.setDescription("Description");
        g.setAssignedToId(assignedTo);
        g.setStatus(status);
        g.setEscalationLevel(level);
        return g;
    }

    private GrievanceAction work(String id, ActionStatus status) {
        GrievanceAction a = new GrievanceAction();
        a.setActionId(id);
        a.setGrievanceId("g1");
        a.setTakenById(FO);
        a.setActionType(ActionType.WK);
        a.setGrievanceActionTitle("Work");
        a.setStatus(status);
        return a;
    }

    private Department department() {
        Department d = new Department("Infrastructure");
        d.setDepartmentId(DEPT);
        d.setDepartmentSupervisorId("sup-1");
        return d;
    }
}
