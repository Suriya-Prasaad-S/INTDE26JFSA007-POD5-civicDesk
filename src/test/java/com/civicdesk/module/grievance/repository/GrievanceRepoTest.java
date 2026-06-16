package com.civicdesk.module.grievance.repository;

import com.civicdesk.module.grievance.entity.Grievance;
import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
// Own isolated in-memory DB so this slice never shares (or drops) the seeded
// `civicdesk_test` schema that the @SpringBootTest IAM integration tests rely on.
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:grievance_repo_test;DB_CLOSE_DELAY=-1")
class GrievanceRepoTest {

    @Autowired
    private GrievanceRepo grievanceRepo;

    @BeforeEach
    void setup() {
        // Citizen c1: two grievances in dep-1, assigned to fo-1 and sup-1 respectively.
        grievanceRepo.save(grievance("c1", "dep-1", "fo-1"));
        grievanceRepo.save(grievance("c1", "dep-1", "sup-1"));
        // Citizen c2: one grievance in dep-2, assigned to fo-1.
        grievanceRepo.save(grievance("c2", "dep-2", "fo-1"));
    }

    @Test
    void findByCitizenId_returnsOnlyThatCitizensGrievances() {
        List<Grievance> result = grievanceRepo.findByCitizenId("c1");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(g -> g.getCitizenId().equals("c1"));
    }

    @Test
    void findByCitizenId_unknownCitizen_returnsEmpty() {
        assertThat(grievanceRepo.findByCitizenId("nobody")).isEmpty();
    }

    @Test
    void findByAssignedToId_returnsOnlyGrievancesAssignedToThatUser() {
        List<Grievance> result = grievanceRepo.findByAssignedToId("fo-1");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(g -> g.getAssignedToId().equals("fo-1"));
    }

    @Test
    void findByAssignedToId_unassignedUser_returnsEmpty() {
        assertThat(grievanceRepo.findByAssignedToId("ghost")).isEmpty();
    }

    @Test
    void findByDepartmentId_returnsOnlyThatDepartmentsGrievances() {
        List<Grievance> result = grievanceRepo.findByDepartmentId("dep-1");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(g -> g.getDepartmentId().equals("dep-1"));
    }

    @Test
    void findByDepartmentId_unknownDepartment_returnsEmpty() {
        assertThat(grievanceRepo.findByDepartmentId("dep-999")).isEmpty();
    }

    private Grievance grievance(String citizenId, String departmentId, String assignedToId) {
        Grievance g = new Grievance();
        g.setCitizenId(citizenId);
        g.setDepartmentId(departmentId);
        g.setAssignedToId(assignedToId);
        g.setCategory(Category.RI);
        g.setGrievanceTitle("Title");
        g.setDescription("Description");
        g.setStatus(GrievanceStatus.O);
        g.setEscalationLevel(EscalationLevel.L2);
        return g;
    }
}
