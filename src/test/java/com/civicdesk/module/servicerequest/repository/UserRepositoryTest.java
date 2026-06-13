package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.external.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test for {@link UserRepository#findByRoleAndStatusAndDepartmentId}, used by
 * officer auto-assignment to find active officers in a department.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private UserRepository repository;

    @BeforeEach
    void seed() {
        em.persist(new User("OFF-1", "Active Officer A", "a@city.gov", "1", "Officer", "DEP-A", "A"));
        em.persist(new User("OFF-2", "Active Officer A2", "b@city.gov", "2", "Officer", "DEP-A", "A"));
        em.persist(new User("OFF-3", "Inactive Officer A", "c@city.gov", "3", "Officer", "DEP-A", "I"));
        em.persist(new User("OFF-4", "Active Officer B", "d@city.gov", "4", "Officer", "DEP-B", "A"));
        em.persist(new User("CIT-1", "Citizen", "e@city.gov", "5", "Citizen", null, "A"));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("returns only active officers in the requested department")
    void findsActiveOfficersInDepartment() {
        assertThat(repository.findByRoleAndStatusAndDepartmentId("Officer", "A", "DEP-A"))
                .extracting(User::getUserId)
                .containsExactlyInAnyOrder("OFF-1", "OFF-2");
    }

    @Test
    @DisplayName("returns empty when no active officer matches")
    void emptyWhenNoMatch() {
        assertThat(repository.findByRoleAndStatusAndDepartmentId("Officer", "A", "DEP-Z")).isEmpty();
        assertThat(repository.findByRoleAndStatusAndDepartmentId("Officer", "I", "DEP-B")).isEmpty();
    }
}
