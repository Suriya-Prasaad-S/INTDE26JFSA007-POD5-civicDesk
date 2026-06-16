package com.civicdesk.module.grievance.repository;

import com.civicdesk.module.grievance.entity.GrievanceAction;
import com.civicdesk.module.grievance.enums.ActionStatus;
import com.civicdesk.module.grievance.enums.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
// Own isolated in-memory DB so this slice never shares (or drops) the seeded
// `civicdesk_test` schema that the @SpringBootTest IAM integration tests rely on.
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:grievance_action_repo_test;DB_CLOSE_DELAY=-1")
class GrievanceActionRepoTest {

    @Autowired
    private GrievanceActionRepo grievanceActionRepo;

    @BeforeEach
    void setup() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 9, 0);
        // Three actions on g1, inserted out of chronological order to prove ordering.
        grievanceActionRepo.save(action("g1", ActionType.WK, "second", base.plusHours(2)));
        grievanceActionRepo.save(action("g1", ActionType.AS, "first", base));
        grievanceActionRepo.save(action("g1", ActionType.RV, "third", base.plusHours(4)));
        // An action on a different grievance — must never leak into g1's timeline.
        grievanceActionRepo.save(action("g2", ActionType.WK, "other", base.plusHours(1)));
    }

    @Test
    void findByGrievanceIdOrderByActionDateAsc_returnsThatGrievancesActionsOldestFirst() {
        List<GrievanceAction> result = grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("g1");

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(a -> a.getGrievanceId().equals("g1"));
        assertThat(result).extracting(GrievanceAction::getGrievanceActionTitle)
                .containsExactly("first", "second", "third");
    }

    @Test
    void findByGrievanceIdOrderByActionDateAsc_unknownGrievance_returnsEmpty() {
        assertThat(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("no-such")).isEmpty();
    }

    private GrievanceAction action(String grievanceId, ActionType type, String title, LocalDateTime date) {
        GrievanceAction a = new GrievanceAction();
        a.setGrievanceId(grievanceId);
        a.setTakenById("u1");
        a.setActionType(type);
        a.setGrievanceActionTitle(title);
        a.setActionDate(date);
        a.setStatus(type == ActionType.WK ? ActionStatus.O : null);
        return a;
    }
}
