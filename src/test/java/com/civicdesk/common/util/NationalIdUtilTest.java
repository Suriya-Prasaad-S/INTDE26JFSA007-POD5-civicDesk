package com.civicdesk.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NationalIdUtilTest {

    @Test
    void hash_isConsistentAnd64HexChars() {
        String a = NationalIdUtil.hash("1234-5678-9012");
        String b = NationalIdUtil.hash("1234-5678-9012");
        assertEquals(a, b);
        assertEquals(64, a.length());
    }

    @Test
    void hash_differsForDifferentInput() {
        assertNotEquals(NationalIdUtil.hash("1234-5678-9012"), NationalIdUtil.hash("0000-0000-0000"));
    }

    @Test
    void hash_blankOrNull_returnsNull() {
        assertNull(NationalIdUtil.hash(null));
        assertNull(NationalIdUtil.hash("   "));
    }

    @Test
    void maskLast4_masksAllButLastFour() {
        assertEquals("********9012", NationalIdUtil.maskLast4("123456789012"));
    }

    @Test
    void maskLast4_shortValue_returnedAsIs() {
        assertEquals("12", NationalIdUtil.maskLast4("12"));
    }
}
