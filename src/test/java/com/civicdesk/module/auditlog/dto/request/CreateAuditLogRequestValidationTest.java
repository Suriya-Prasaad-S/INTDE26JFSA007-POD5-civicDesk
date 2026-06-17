package com.civicdesk.module.auditlog.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bean-validation coverage for {@link CreateAuditLogRequest}: every constraint must
 * fire on the offending field with the documented message.
 */
class CreateAuditLogRequestValidationTest {

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

    private CreateAuditLogRequest valid() {
        CreateAuditLogRequest req = new CreateAuditLogRequest();
        req.setUserId("10000001");
        req.setAction("LOGIN");
        req.setModule("IAM");
        return req;
    }

    private boolean hasMessage(Set<ConstraintViolation<CreateAuditLogRequest>> v, String fragment) {
        return v.stream().anyMatch(cv -> cv.getMessage().contains(fragment));
    }

    @Test
    void validRequest_hasNoViolations() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void lowercaseActionAndModule_areValid() {
        CreateAuditLogRequest req = valid();
        req.setAction("login");
        req.setModule("iam");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void maxLengthUserId_isValid() {
        CreateAuditLogRequest req = valid();
        req.setUserId("12345678901234567890"); // 20 chars
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void nullUserId_fails() {
        CreateAuditLogRequest req = valid();
        req.setUserId(null);
        assertTrue(hasMessage(validator.validate(req), "userId is required"));
    }

    @Test
    void blankUserId_fails() {
        CreateAuditLogRequest req = valid();
        req.setUserId("  ");
        assertTrue(hasMessage(validator.validate(req), "userId is required"));
    }

    @Test
    void userIdTooLong_fails() {
        CreateAuditLogRequest req = valid();
        req.setUserId("123456789012345678901"); // 21 chars
        assertTrue(hasMessage(validator.validate(req), "userId must not exceed 20 characters"));
    }

    @Test
    void nullAction_fails() {
        CreateAuditLogRequest req = valid();
        req.setAction(null);
        assertTrue(hasMessage(validator.validate(req), "action is required"));
    }

    @Test
    void invalidAction_fails() {
        CreateAuditLogRequest req = valid();
        req.setAction("WIZARDRY");
        assertTrue(hasMessage(validator.validate(req), "action must be a valid audit action"));
    }

    @Test
    void nullModule_fails() {
        CreateAuditLogRequest req = valid();
        req.setModule(null);
        assertTrue(hasMessage(validator.validate(req), "module is required"));
    }

    @Test
    void invalidModule_fails() {
        CreateAuditLogRequest req = valid();
        req.setModule("ATLANTIS");
        assertTrue(hasMessage(validator.validate(req), "module must be a valid audit module"));
    }

    @Test
    void multipleInvalidFields_reportAllViolations() {
        CreateAuditLogRequest req = new CreateAuditLogRequest();
        req.setUserId(null);
        req.setAction("NOPE");
        req.setModule("NOPE");
        assertEquals(3, validator.validate(req).size());
    }

    @Test
    void gettersAndSettersRoundTrip() {
        CreateAuditLogRequest req = new CreateAuditLogRequest();
        req.setUserId("u");
        req.setAction("a");
        req.setModule("m");
        assertEquals("u", req.getUserId());
        assertEquals("a", req.getAction());
        assertEquals("m", req.getModule());
    }
}
