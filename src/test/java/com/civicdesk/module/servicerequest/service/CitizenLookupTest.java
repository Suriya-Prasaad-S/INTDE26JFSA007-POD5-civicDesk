package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.ForbiddenException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.entity.external.User;
import com.civicdesk.module.serviceRequest.repository.CitizenProfileRepository;
import com.civicdesk.module.serviceRequest.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CitizenLookup}: a citizen may submit only if their profile exists,
 * their linked account exists, and that account is not Flagged ("F").
 */
@ExtendWith(MockitoExtension.class)
class CitizenLookupTest {

    @Mock private CitizenProfileRepository citizenProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CitizenLookup citizenLookup;

    private CitizenProfile citizen() {
        return new CitizenProfile("CIT-1", "USR-1", "NID-1", "addr", "W1", "Z1");
    }

    private User account(String status) {
        return new User("USR-1", "Carl Citizen", "carl@mail.com", "555", "Citizen", null, status);
    }

    @Test
    @DisplayName("returns the profile for an Active citizen")
    void activeCitizen() {
        when(citizenProfileRepository.findById("CIT-1")).thenReturn(Optional.of(citizen()));
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(account("A")));

        CitizenProfile result = citizenLookup.loadSubmittableCitizen("CIT-1");

        assertThat(result.getCitizenId()).isEqualTo("CIT-1");
    }

    @Test
    @DisplayName("throws when the citizen profile is missing")
    void citizenNotFound() {
        when(citizenProfileRepository.findById("CIT-X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citizenLookup.loadSubmittableCitizen("CIT-X"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CIT-X");
    }

    @Test
    @DisplayName("throws when the linked user account is missing")
    void userNotFound() {
        when(citizenProfileRepository.findById("CIT-1")).thenReturn(Optional.of(citizen()));
        when(userRepository.findById("USR-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citizenLookup.loadSubmittableCitizen("CIT-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("forbids a Flagged citizen from submitting")
    void flaggedCitizen() {
        when(citizenProfileRepository.findById("CIT-1")).thenReturn(Optional.of(citizen()));
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(account("F")));

        assertThatThrownBy(() -> citizenLookup.loadSubmittableCitizen("CIT-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("flagged");
    }
}
