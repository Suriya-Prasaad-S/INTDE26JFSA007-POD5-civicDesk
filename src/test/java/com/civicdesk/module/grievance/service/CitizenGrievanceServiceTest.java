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
import com.civicdesk.module.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceDetailsUpdateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceReopenReq;
import com.civicdesk.module.grievance.dto.response.GrievanceResponse;
import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;
import com.civicdesk.module.grievance.mapper.GrievanceMapper;
import com.civicdesk.module.grievance.repository.GrievanceActionRepo;
import com.civicdesk.module.grievance.repository.GrievanceRepo;
import com.civicdesk.module.iam.entity.Department;
import com.civicdesk.module.iam.repository.DepartmentRepository;

/** Unit tests for the citizen-side {@link CitizenGrievanceService}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitizenGrievanceServiceTest {

    private static final String CITIZEN = "citizen-1";

    @Mock GrievanceRepo grievanceRepo;
    @Mock GrievanceActionRepo grievanceActionRepo;
    @Mock DepartmentRepository departmentRepository;
    GrievanceMapper mapper = new GrievanceMapper();

    CitizenGrievanceService service;

    @BeforeEach
    void setup() {
        service = new CitizenGrievanceService(grievanceRepo, grievanceActionRepo, departmentRepository, mapper);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CITIZEN, null,
                        List.of(new SimpleGrantedAuthority("ROLE_CIT"))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // --- create ---

    @Test
    void createGrievance_setsDefaults_andSaves() {
        when(departmentRepository.findByName("Infrastructure")).thenReturn(Optional.of(department()));
        when(grievanceRepo.save(any(Grievance.class))).thenAnswer(i -> i.getArgument(0));

        GrievanceResponse res = service.createGrievance(createReq("RI"));

        assertThat(res.getCategory()).isEqualTo(Category.RI);
        assertThat(res.getStatus()).isEqualTo(GrievanceStatus.O);
        assertThat(res.getEscalationLevel()).isEqualTo(EscalationLevel.L2);
        verify(grievanceRepo).save(any(Grievance.class));
    }

    @Test
    void createGrievance_invalidCategory_throwsBadRequest() {
        assertThatThrownBy(() -> service.createGrievance(createReq("XX")))
                .isInstanceOf(InvalidGrievanceDataException.class);
    }

    @Test
    void createGrievance_departmentNotConfigured_throwsBadRequest() {
        when(departmentRepository.findByName("Infrastructure")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createGrievance(createReq("RI")))
                .isInstanceOf(InvalidGrievanceDataException.class);
    }

    // --- update ---

    @Test
    void updateDetails_whileOpen_updates() {
        Grievance g = grievance(GrievanceStatus.O);
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(g));
        when(grievanceRepo.save(any(Grievance.class))).thenAnswer(i -> i.getArgument(0));

        GrievanceDetailsUpdateReq req = new GrievanceDetailsUpdateReq();
        req.setGrievanceTitle("new title");
        req.setDescription("new description");

        GrievanceResponse res = service.updateGrievanceDetails("g1", req);

        assertThat(res.getGrievanceTitle()).isEqualTo("new title");
        assertThat(res.getDescription()).isEqualTo("new description");
    }

    @Test
    void updateDetails_notOpen_throwsConflict() {
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(grievance(GrievanceStatus.IP)));
        assertThatThrownBy(() -> service.updateGrievanceDetails("g1", detailsReq()))
                .isInstanceOf(InvalidGrievanceStateException.class);
    }

    @Test
    void updateDetails_notOwner_throwsForbidden() {
        Grievance g = grievance(GrievanceStatus.O);
        g.setCitizenId("someone-else");
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(g));
        assertThatThrownBy(() -> service.updateGrievanceDetails("g1", detailsReq()))
                .isInstanceOf(UnauthorizedGrievanceAccessException.class);
    }

    @Test
    void updateDetails_notFound_throwsNotFound() {
        when(grievanceRepo.findById("g1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateGrievanceDetails("g1", detailsReq()))
                .isInstanceOf(GrievanceNotFoundException.class);
    }

    // --- read ---

    @Test
    void getMyGrievances_returnsCallersList() {
        when(grievanceRepo.findByCitizenId(CITIZEN)).thenReturn(List.of(grievance(GrievanceStatus.O)));
        assertThat(service.getMyGrievances()).hasSize(1);
    }

    @Test
    void getGrievanceById_returnsDetail() {
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(grievance(GrievanceStatus.O)));
        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1")).thenReturn(List.of());

        var detail = service.getGrievanceById("g1");

        assertThat(detail.getGrievance()).isNotNull();
        assertThat(detail.getActions()).isEmpty();
    }

    @Test
    void getGrievanceById_notOwner_throwsForbidden() {
        Grievance g = grievance(GrievanceStatus.O);
        g.setCitizenId("someone-else");
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(g));
        assertThatThrownBy(() -> service.getGrievanceById("g1"))
                .isInstanceOf(UnauthorizedGrievanceAccessException.class);
    }

    // --- close ---

    @Test
    void close_whenResolved_setsClosed_andLogsAction() {
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(grievance(GrievanceStatus.R)));
        when(grievanceRepo.save(any(Grievance.class))).thenAnswer(i -> i.getArgument(0));

        GrievanceResponse res = service.closeGrievance("g1");

        assertThat(res.getStatus()).isEqualTo(GrievanceStatus.C);
        verify(grievanceActionRepo).save(any(GrievanceAction.class));
    }

    @Test
    void close_notResolved_throwsConflict() {
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(grievance(GrievanceStatus.O)));
        assertThatThrownBy(() -> service.closeGrievance("g1"))
                .isInstanceOf(InvalidGrievanceStateException.class);
    }

    // --- reopen ---

    @Test
    void reopen_whenResolved_setsReopened_backToSupervisor_andLogsAction() {
        Grievance g = grievance(GrievanceStatus.R);
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(g));
        when(departmentRepository.findById("dep-1")).thenReturn(Optional.of(department()));
        when(grievanceRepo.save(any(Grievance.class))).thenAnswer(i -> i.getArgument(0));

        GrievanceReopenReq req = new GrievanceReopenReq();
        req.setReason("Not actually fixed");

        GrievanceResponse res = service.reopenGrievance("g1", req);

        assertThat(res.getStatus()).isEqualTo(GrievanceStatus.RO);
        assertThat(res.getEscalationLevel()).isEqualTo(EscalationLevel.L2);
        assertThat(g.getAssignedToId()).isEqualTo("sup-1");
        verify(grievanceActionRepo).save(any(GrievanceAction.class));
    }

    @Test
    void reopen_notResolved_throwsConflict() {
        when(grievanceRepo.findById("g1")).thenReturn(Optional.of(grievance(GrievanceStatus.O)));
        GrievanceReopenReq req = new GrievanceReopenReq();
        req.setReason("x");
        assertThatThrownBy(() -> service.reopenGrievance("g1", req))
                .isInstanceOf(InvalidGrievanceStateException.class);
    }

    // --- fixtures ---

    private GrievanceCreateReq createReq(String category) {
        GrievanceCreateReq req = new GrievanceCreateReq();
        req.setCategory(category);
        req.setGrievanceTitle("Pothole");
        req.setDescription("Deep pothole near the bus stop");
        req.setWard("Ward 12");
        return req;
    }

    private GrievanceDetailsUpdateReq detailsReq() {
        GrievanceDetailsUpdateReq req = new GrievanceDetailsUpdateReq();
        req.setGrievanceTitle("t");
        req.setDescription("d");
        return req;
    }

    private Grievance grievance(GrievanceStatus status) {
        Grievance g = new Grievance();
        g.setGrievanceId("g1");
        g.setCitizenId(CITIZEN);
        g.setDepartmentId("dep-1");
        g.setCategory(Category.RI);
        g.setGrievanceTitle("Title");
        g.setDescription("Description");
        g.setStatus(status);
        g.setEscalationLevel(EscalationLevel.L2);
        return g;
    }

    private Department department() {
        Department d = new Department("Infrastructure");
        d.setDepartmentId("dep-1");
        d.setDepartmentSupervisorId("sup-1");
        return d;
    }
}
