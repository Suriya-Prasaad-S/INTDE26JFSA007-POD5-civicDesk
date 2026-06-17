package com.civicdesk.module.citizen.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit tests for the {@link CitizenStatus} code mapping. */
class CitizenStatusTest {

    @Test
    void getCode_returnsSingleCharacterCode() {
        assertThat(CitizenStatus.Active.getCode()).isEqualTo("A");
        assertThat(CitizenStatus.Verified.getCode()).isEqualTo("V");
        assertThat(CitizenStatus.Flagged.getCode()).isEqualTo("F");
    }

    @Test
    void fromCode_mapsKnownCode() {
        assertThat(CitizenStatus.fromCode("A")).isEqualTo(CitizenStatus.Active);
        assertThat(CitizenStatus.fromCode("V")).isEqualTo(CitizenStatus.Verified);
        assertThat(CitizenStatus.fromCode("F")).isEqualTo(CitizenStatus.Flagged);
    }

    @Test
    void fromCode_isCaseInsensitive() {
        assertThat(CitizenStatus.fromCode("v")).isEqualTo(CitizenStatus.Verified);
    }

    @Test
    void fromCode_unknownCode_throwsIllegalArgument() {
        assertThatThrownBy(() -> CitizenStatus.fromCode("Z"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromCode_null_throws() {
        assertThatThrownBy(() -> CitizenStatus.fromCode(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void allowedCodes_listsEveryCode() {
        assertThat(CitizenStatus.allowedCodes()).isEqualTo("A, V, F");
    }
}
