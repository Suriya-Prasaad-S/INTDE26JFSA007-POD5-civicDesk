package com.civicdesk.module.auditlog.validation;

import com.civicdesk.module.auditlog.enums.AuditAction;
import com.civicdesk.module.auditlog.enums.AuditModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link EnumValid}/{@link EnumValidator} through the Bean Validation runtime
 * using small holder classes that carry the constraint in different configurations.
 */
class EnumValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    static class CaseInsensitiveHolder {
        @EnumValid(enumClass = AuditAction.class, message = "bad action")
        String value;

        CaseInsensitiveHolder(String value) {
            this.value = value;
        }
    }

    static class CaseSensitiveHolder {
        @EnumValid(enumClass = AuditModule.class, ignoreCase = false, message = "bad module")
        String value;

        CaseSensitiveHolder(String value) {
            this.value = value;
        }
    }

    private int violations(Object holder) {
        return validator.validate(holder).size();
    }

    @Test
    void exactEnumName_passes() {
        assertEquals(0, violations(new CaseInsensitiveHolder("LOGIN")));
    }

    @Test
    void lowercaseName_passesWhenIgnoreCase() {
        assertEquals(0, violations(new CaseInsensitiveHolder("login")));
    }

    @Test
    void mixedCaseName_passesWhenIgnoreCase() {
        assertEquals(0, violations(new CaseInsensitiveHolder("LoGiN")));
    }

    @Test
    void valueWithSurroundingWhitespace_isTrimmedAndPasses() {
        assertEquals(0, violations(new CaseInsensitiveHolder("  LOGIN  ")));
    }

    @Test
    void unknownName_fails() {
        assertEquals(1, violations(new CaseInsensitiveHolder("WIZARDRY")));
    }

    @Test
    void unknownName_carriesConfiguredMessage() {
        var result = validator.validate(new CaseInsensitiveHolder("WIZARDRY"));
        assertEquals("bad action", result.iterator().next().getMessage());
    }

    @Test
    void nullValue_passes() {
        assertEquals(0, violations(new CaseInsensitiveHolder(null)));
    }

    @Test
    void blankValue_passes() {
        assertEquals(0, violations(new CaseInsensitiveHolder("   ")));
    }

    @Test
    void caseSensitive_exactName_passes() {
        assertEquals(0, violations(new CaseSensitiveHolder("IAM")));
    }

    @Test
    void caseSensitive_lowercase_fails() {
        assertEquals(1, violations(new CaseSensitiveHolder("iam")));
    }

    @Test
    void caseSensitive_unknown_fails() {
        assertTrue(violations(new CaseSensitiveHolder("ATLANTIS")) >= 1);
    }

    @Test
    void differentEnum_validValuePasses() {
        assertEquals(0, violations(new CaseSensitiveHolder("GRIEVANCE")));
    }
}
