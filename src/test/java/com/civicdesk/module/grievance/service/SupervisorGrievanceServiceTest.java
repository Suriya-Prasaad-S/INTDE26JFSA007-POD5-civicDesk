package com.civicdesk.module.grievance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.civicdesk.common.exception.grievance.GrievanceNotFoundException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceDataException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceStateException;
import com.civicdesk.common.exception.grievance.UnauthorizedGrievanceAccessException;
import com.civicdesk.module.grievance.dto.request.AssignFieldOfficerReq;
import com.civicdesk.module.grievance.dto.request.ResolveReq;
import com.civicdesk.module.grievance.dto.response.GrievanceSummaryResponse;
import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;
import com.civicdesk.module.grievance.mapper.GrievanceMapper;
import com.civicdesk.module.grievance.repository.GrievanceActionRepo;
import com.civicdesk.module.grievance.repository.GrievanceRepo;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.UserRepository;

/** Unit tests for the department-supervisor {@link SupervisorGrievanceService}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupervisorGrievanceServiceTest {

    private static final String SUP = "sup-1";
    private static final String DEPT = "dep-1";
    private static final String FO = "fo-1";

    @Mock GrievanceRepo grievanceRepo;
    @Mock GrievanceActionRepo grievanceActionRepo;
    @Mock UserRepository userRepository;
    GrievanceMapper mapper = new GrievanceMapper();

    SupervisorGrievanceService service;

    @BeforeEach
    void setup() {
        service = new SupervisorGrievanceService(grievanceRepo, grievanceActionRepo, mapper, userRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SUP, null,
                        List.of(new SimpleGrantedAuthority("ROLE_DS"))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // --- department resolution ---

    @Test
    void getDepartmentGrievances_returnsDeptQueue() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findByDepartmentId(DEPT))
                .thenReturn(List.of(deptGrievance(GrievanceStatus.O, EscalationLevel.L2)));
        assertThat(service.getDepartmentGrievances()).hasSize(1);
    }

    @Test
    void supervisorWithoutDepartment_throwsForbidden() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(null)));
        assertThatThrownBy(() -> service.getDepartmentGrievances())
                .isInstanceOf(UnauthorizedGrievanceAccessException.class);
    }

    @Test
    void supervisorAccountMissing_throwsForbidden() {
        when(userRepository.findById(SUP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDepartmentGrievances())
                .isInstanceOf(UnauthorizedGrievanceAccessException.class);
    }

    // --- assign ---

    @Test
    void assign_validFieldOfficer_assignsAndLogs() {
        Grievance g = deptGrievance(GrievanceStatus.O, EscalationLevel.L2);
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(g));
        when(userRepository.findById(FO))
                .thenReturn(Optional.of(user(FO, Role.FO.name(), DEPT, UserStatus.ACT.getLabel())));
        when(grievanceRepo.save(any(Grievance.class))).thenAnswer(i -> i.getArgument(0));

        GrievanceSummaryResponse res = service.assignFieldOfficer("g1", assignReq());

        assertThat(res.getStatus()).isEqualTo(GrievanceStatus.IP);
        assertThat(res.getEscalationLevel()).isEqualTo(EscalationLevel.L1);
        assertThat(g.getFieldOfficerId()).isEqualTo(FO);
        assertThat(g.getAssignedToId()).isEqualTo(FO);
        verify(grievanceActionRepo).save(any(GrievanceAction.class));
    }

    @Test
    void assign_grievanceNotInDept_throwsForbidden() {
        Grievance g = deptGrievance(GrievanceStatus.O, EscalationLevel.L2);
        g.setDepartmentId("other-dept");
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(g));
        assertThatThrownBy(() -> service.assignFieldOfficer("g1", assignReq()))
                .isInstanceOf(UnauthorizedGrievanceAccessException.class);
    }

    @Test
    void assign_notWithSupervisor_throwsConflict() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(deptGrievance(GrievanceStatus.IP, EscalationLevel.L1))); // with FO
        assertThatThrownBy(() -> service.assignFieldOfficer("g1", assignReq()))
                .isInstanceOf(InvalidGrievanceStateException.class);
    }

    @Test
    void assign_userNotFieldOfficer_throwsBadRequest() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(deptGrievance(GrievanceStatus.O, EscalationLevel.L2)));
        when(userRepository.findById(FO))
                .thenReturn(Optional.of(user(FO, Role.DS.name(), DEPT, UserStatus.ACT.getLabel())));
        assertThatThrownBy(() -> service.assignFieldOfficer("g1", assignReq()))
                .isInstanceOf(InvalidGrievanceDataException.class);
    }

    @Test
    void assign_fieldOfficerInDifferentDept_throwsBadRequest() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(deptGrievance(GrievanceStatus.O, EscalationLevel.L2)));
        when(userRepository.findById(FO))
                .thenReturn(Optional.of(user(FO, Role.FO.name(), "other-dept", UserStatus.ACT.getLabel())));
        assertThatThrownBy(() -> service.assignFieldOfficer("g1", assignReq()))
                .isInstanceOf(InvalidGrievanceDataException.class);
    }

    @Test
    void assign_inactiveFieldOfficer_throwsBadRequest() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(deptGrievance(GrievanceStatus.O, EscalationLevel.L2)));
        when(userRepository.findById(FO))
                .thenReturn(Optional.of(user(FO, Role.FO.name(), DEPT, "INACTIVE")));
        assertThatThrownBy(() -> service.assignFieldOfficer("g1", assignReq()))
                .isInstanceOf(InvalidGrievanceDataException.class);
    }

    @Test
    void assign_fieldOfficerNotFound_throwsBadRequest() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(deptGrievance(GrievanceStatus.O, EscalationLevel.L2)));
        when(userRepository.findById(FO)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assignFieldOfficer("g1", assignReq()))
                .isInstanceOf(InvalidGrievanceDataException.class);
    }

    // --- resolve ---

    @Test
    void resolve_whenWithSupervisor_setsResolved_andLogs() {
        Grievance g = deptGrievance(GrievanceStatus.IP, EscalationLevel.L2);
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(g));
        when(grievanceRepo.save(any(Grievance.class))).thenAnswer(i -> i.getArgument(0));

        GrievanceSummaryResponse res = service.resolveGrievance("g1", resolveReq());

        assertThat(res.getStatus()).isEqualTo(GrievanceStatus.R);
        verify(grievanceActionRepo).save(any(GrievanceAction.class));
    }

    @Test
    void resolve_notWithSupervisor_throwsConflict() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(deptGrievance(GrievanceStatus.IP, EscalationLevel.L1)));
        assertThatThrownBy(() -> service.resolveGrievance("g1", resolveReq()))
                .isInstanceOf(InvalidGrievanceStateException.class);
    }

    @Test
    void resolve_grievanceNotFound_throwsNotFound() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolveGrievance("g1", resolveReq()))
                .isInstanceOf(GrievanceNotFoundException.class);
    }

    // --- view ---

    @Test
    void viewDepartmentGrievance_returnsDetail() {
        when(userRepository.findById(SUP)).thenReturn(Optional.of(supervisor(DEPT)));
        when(grievanceRepo.findById("g1"))
                .thenReturn(Optional.of(deptGrievance(GrievanceStatus.IP, EscalationLevel.L2)));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1")).thenReturn(List.of());

        var detail = service.viewDepartmentGrievance("g1");

        assertThat(detail.getGrievance()).isNotNull();
    }

    // --- fixtures ---

    private AssignFieldOfficerReq assignReq() {
        AssignFieldOfficerReq req = new AssignFieldOfficerReq();
        req.setFieldOfficerId(FO);
        req.setMessage("Please inspect and fix.");
        return req;
    }

    private ResolveReq resolveReq() {
        ResolveReq req = new ResolveReq();
        req.setMessage("Verified and fixed.");
        return req;
    }

    private User supervisor(String deptId) {
        return user(SUP, Role.DS.name(), deptId, UserStatus.ACT.getLabel());
    }

    private User user(String id, String role, String deptId, String status) {
        User u = new User();
        u.setUserId(id);
        u.setRole(role);
        u.setDepartmentId(deptId);
        u.setStatus(status);
        return u;
    }

    private Grievance deptGrievance(GrievanceStatus status, EscalationLevel level) {
        Grievance g = new Grievance();
        g.setGrievanceId("g1");
        g.setCitizenId("c1");
        g.setDepartmentId(DEPT);
        g.setCategory(Category.RI);
        g.setGrievanceTitle("Title");
        g.setDescription("Description");
        g.setStatus(status);
        g.setEscalationLevel(level);
        return g;
    }
}
