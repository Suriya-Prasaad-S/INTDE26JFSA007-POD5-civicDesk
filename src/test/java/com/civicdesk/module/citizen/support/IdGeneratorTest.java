package com.civicdesk.module.citizen.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link IdGenerator}. */
class IdGeneratorTest {

    @Test
    void newId_hasExpectedLength() {
        assertThat(IdGenerator.newId()).hasSize(IdGenerator.ID_LENGTH);
    }

    @Test
    void newId_isAlphanumericOnly() {
        for (int i = 0; i < 100; i++) {
            assertThat(IdGenerator.newId()).matches("[A-Za-z0-9]{16}");
        }
    }

    @Test
    void newId_isEffectivelyUnique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(IdGenerator.newId());
        }
        assertThat(ids).hasSize(10_000);
    }
}
