package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.UnprocessableEntityException;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.repository.UserRepository;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.repository.ServiceRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OfficerAssignment}: picks the active officer in the department with
 * the fewest non-terminal requests, and fails when no active officer is available.
 */
@ExtendWith(MockitoExtension.class)
class OfficerAssignmentTest {

    @Mock private UserRepository userRepository;
    @Mock private ServiceRequestRepository requestRepository;

    @InjectMocks private OfficerAssignment officerAssignment;

    private User officer(String id) {
        User u = new User();
        u.setUserId(id);
        u.setName("Officer " + id);
        u.setEmail(id + "@city.gov");
        u.setPhone("555");
        u.setRole("FO");
        u.setDepartmentId("DEP-1");
        u.setStatus("A");
        return u;
    }

    @Test
    @DisplayName("selects the officer with the smallest active workload")
    void selectsLeastLoaded() {
        User busy = officer("OFF-1");
        User idle = officer("OFF-2");
        when(userRepository.findByRoleAndStatusAndDepartmentId("FO", "A", "DEP-1"))
                .thenReturn(List.of(busy, idle));
        when(requestRepository.countByAssignedOfficerAndStatusNotIn(eq(busy), anyCollection()))
                .thenReturn(5L);
        when(requestRepository.countByAssignedOfficerAndStatusNotIn(eq(idle), anyCollection()))
                .thenReturn(1L);

        User chosen = officerAssignment.findLeastLoadedOfficer("DEP-1");

        assertThat(chosen).isSameAs(idle);
    }

    @Test
    @DisplayName("counts workload excluding terminal (Completed/Rejected) requests")
    void excludesTerminalStatesFromWorkload() {
        // Two officers so Stream.min actually invokes the comparator (and thus the count query).
        User first = officer("OFF-1");
        User second = officer("OFF-2");
        when(userRepository.findByRoleAndStatusAndDepartmentId("FO", "A", "DEP-1"))
                .thenReturn(List.of(first, second));
        when(requestRepository.countByAssignedOfficerAndStatusNotIn(any(User.class), anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<RequestStatus> excluded = invocation.getArgument(1);
                    assertThat(excluded).containsExactlyInAnyOrder(
                            RequestStatus.Completed, RequestStatus.Rejected);
                    return 0L;
                });

        officerAssignment.findLeastLoadedOfficer("DEP-1");
    }

    @Test
    @DisplayName("throws when no active officer exists in the department")
    void noOfficerAvailable() {
        when(userRepository.findByRoleAndStatusAndDepartmentId("FO", "A", "DEP-1"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> officerAssignment.findLeastLoadedOfficer("DEP-1"))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("DEP-1");
    }
}
