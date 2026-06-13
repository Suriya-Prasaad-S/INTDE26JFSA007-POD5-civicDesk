package com.civicdesk.module.serviceRequest.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the {@link RequestStatus} workflow: code mapping, terminal detection, and the
 * allowed-transition table that {@code ServiceRequestService.updateRequestStatus} enforces.
 */
class RequestStatusTest {

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    @DisplayName("each status round-trips through its code")
    void codeRoundTrip(RequestStatus status) {
        assertThat(RequestStatus.fromValue(status.getCode())).isEqualTo(status);
    }

    @Test
    @DisplayName("fromValue accepts both the code and the full name, case-insensitively")
    void fromValueAcceptsCodeAndName() {
        assertThat(RequestStatus.fromValue("S")).isEqualTo(RequestStatus.Submitted);
        assertThat(RequestStatus.fromValue("submitted")).isEqualTo(RequestStatus.Submitted);
        assertThat(RequestStatus.fromValue(null)).isNull();
    }

    @Test
    @DisplayName("fromValue rejects an unknown value")
    void fromValueRejectsUnknown() {
        assertThatThrownBy(() -> RequestStatus.fromValue("Z"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("only Completed and Rejected are terminal")
    void terminalStates() {
        assertThat(RequestStatus.Completed.isTerminal()).isTrue();
        assertThat(RequestStatus.Rejected.isTerminal()).isTrue();
        assertThat(RequestStatus.Submitted.isTerminal()).isFalse();
        assertThat(RequestStatus.UnderReview.isTerminal()).isFalse();
        assertThat(RequestStatus.PendingDocuments.isTerminal()).isFalse();
        assertThat(RequestStatus.Approved.isTerminal()).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "Submitted, UnderReview",
            "UnderReview, PendingDocuments",
            "UnderReview, Approved",
            "UnderReview, Rejected",
            "PendingDocuments, UnderReview",
            "PendingDocuments, Rejected",
            "Approved, Completed",
            "Approved, Rejected"
    })
    void allowedTransitions(RequestStatus from, RequestStatus to) {
        assertThat(from.allowedNextStates()).contains(to);
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "Submitted, Approved",
            "Submitted, Completed",
            "Submitted, Rejected",
            "UnderReview, Completed",
            "Approved, UnderReview",
            "PendingDocuments, Approved"
    })
    void disallowedTransitions(RequestStatus from, RequestStatus to) {
        assertThat(from.allowedNextStates()).doesNotContain(to);
    }

    @Test
    @DisplayName("terminal states allow no further transitions")
    void terminalStatesHaveNoNextStates() {
        assertThat(RequestStatus.Completed.allowedNextStates()).isEmpty();
        assertThat(RequestStatus.Rejected.allowedNextStates()).isEmpty();
    }
}
