package com.civicdesk.module.auditlog.dto.response;

import com.civicdesk.module.auditlog.entity.AuditLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditLogResponseTest {

    private AuditLog entity(String ip) {
        AuditLog log = new AuditLog();
        log.setAuditId("10000010");
        log.setUserId("10000001");
        log.setAction("LOGIN");
        log.setModule("IAM");
        log.setIpAddress(ip);
        log.setTimestamp(LocalDateTime.of(2026, 6, 16, 10, 0));
        return log;
    }

    @Test
    void from_mapsAllFields() {
        AuditLogResponse response = AuditLogResponse.from(entity("127.0.0.1"));
        assertEquals("10000010", response.getAuditId());
        assertEquals("10000001", response.getUserId());
        assertEquals("LOGIN", response.getAction());
        assertEquals("IAM", response.getModule());
        assertEquals("127.0.0.1", response.getIpAddress());
        assertEquals(LocalDateTime.of(2026, 6, 16, 10, 0), response.getTimestamp());
    }

    @Test
    void from_preservesNullIpAddress() {
        AuditLogResponse response = AuditLogResponse.from(entity(null));
        assertNull(response.getIpAddress());
    }

    @Test
    void allArgsConstructor_setsFields() {
        AuditLogResponse response = new AuditLogResponse(
                "1", "2", "LOGOUT", "GRIEVANCE", "10.0.0.1", LocalDateTime.of(2026, 1, 1, 0, 0));
        assertEquals("1", response.getAuditId());
        assertEquals("2", response.getUserId());
        assertEquals("LOGOUT", response.getAction());
        assertEquals("GRIEVANCE", response.getModule());
        assertEquals("10.0.0.1", response.getIpAddress());
    }

    @Test
    void settersRoundTrip() {
        AuditLogResponse response = new AuditLogResponse();
        response.setAuditId("a");
        response.setUserId("u");
        response.setAction("LOGIN");
        response.setModule("IAM");
        response.setIpAddress("ip");
        LocalDateTime ts = LocalDateTime.of(2026, 6, 16, 12, 0);
        response.setTimestamp(ts);
        assertEquals("a", response.getAuditId());
        assertEquals("u", response.getUserId());
        assertEquals("LOGIN", response.getAction());
        assertEquals("IAM", response.getModule());
        assertEquals("ip", response.getIpAddress());
        assertEquals(ts, response.getTimestamp());
    }
}
