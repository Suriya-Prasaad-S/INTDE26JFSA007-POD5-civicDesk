package com.civicdesk.module.citizen.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit tests for the {@link DocumentStatus} code mapping. */
class DocumentStatusTest {

    @Test
    void getCode_returnsSingleCharacterCode() {
        assertThat(DocumentStatus.Valid.getCode()).isEqualTo("V");
        assertThat(DocumentStatus.Expired.getCode()).isEqualTo("E");
        assertThat(DocumentStatus.Revoked.getCode()).isEqualTo("R");
    }

    @Test
    void fromCode_mapsKnownCode() {
        assertThat(DocumentStatus.fromCode("V")).isEqualTo(DocumentStatus.Valid);
        assertThat(DocumentStatus.fromCode("E")).isEqualTo(DocumentStatus.Expired);
        assertThat(DocumentStatus.fromCode("R")).isEqualTo(DocumentStatus.Revoked);
    }

    @Test
    void fromCode_isCaseInsensitive() {
        assertThat(DocumentStatus.fromCode("r")).isEqualTo(DocumentStatus.Revoked);
    }

    @Test
    void fromCode_unknownCode_throwsIllegalArgument() {
        assertThatThrownBy(() -> DocumentStatus.fromCode("Z"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromCode_null_throws() {
        assertThatThrownBy(() -> DocumentStatus.fromCode(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void allowedCodes_listsEveryCode() {
        assertThat(DocumentStatus.allowedCodes()).isEqualTo("V, E, R");
    }
}
