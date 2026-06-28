package com.civicdesk.module.auditlog.client;

import com.civicdesk.module.auditlog.enums.AuditAction;
import com.civicdesk.module.auditlog.enums.AuditModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class AuditClientTest {

    private static final String BASE_URL = "http://audit.test";

    private MockRestServiceServer server;
    private AuditClient auditClient;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        auditClient = new AuditClient(builder, BASE_URL);
    }

    @Test
    void record_postsToAuditLogsEndpoint() {
        server.expect(requestTo(BASE_URL + "/audit/auditLogs"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        auditClient.record("10000001", "LOGIN", "IAM", "Bearer token");

        server.verify();
    }

    @Test
    void record_sendsBodyFields() {
        server.expect(requestTo(BASE_URL + "/audit/auditLogs"))
                .andExpect(jsonPath("$.userId").value("10000001"))
                .andExpect(jsonPath("$.action").value("LOGIN"))
                .andExpect(jsonPath("$.module").value("IAM"))
                .andRespond(withStatus(HttpStatus.CREATED));

        auditClient.record("10000001", "LOGIN", "IAM", "Bearer token");

        server.verify();
    }

    @Test
    void record_forwardsAuthorizationHeader() {
        server.expect(requestTo(BASE_URL + "/audit/auditLogs"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andRespond(withStatus(HttpStatus.CREATED));

        auditClient.record("10000001", "LOGIN", "IAM", "Bearer abc.def.ghi");

        server.verify();
    }

    @Test
    void record_setsJsonContentType() {
        server.expect(requestTo(BASE_URL + "/audit/auditLogs"))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andRespond(withStatus(HttpStatus.CREATED));

        auditClient.record("10000001", "LOGIN", "IAM", "Bearer token");

        server.verify();
    }

    @Test
    void record_enumOverload_usesEnumNames() {
        server.expect(requestTo(BASE_URL + "/audit/auditLogs"))
                .andExpect(jsonPath("$.action").value("CREATE_USER"))
                .andExpect(jsonPath("$.module").value("GRIEVANCE"))
                .andRespond(withStatus(HttpStatus.CREATED));

        auditClient.record("10000001", AuditAction.CREATE_USER, AuditModule.GRIEVANCE, "Bearer token");

        server.verify();
    }

    @Test
    void record_serverError_isSwallowed() {
        server.expect(requestTo(BASE_URL + "/audit/auditLogs"))
                .andRespond(withServerError());

        // Audit failures must never propagate to the caller's business flow.
        assertDoesNotThrow(() -> auditClient.record("10000001", "LOGIN", "IAM", "Bearer token"));

        server.verify();
    }

    @Test
    void record_clientError_isSwallowed() {
        server.expect(requestTo(BASE_URL + "/audit/auditLogs"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertDoesNotThrow(() -> auditClient.record("10000001", "BOGUS", "IAM", "Bearer token"));

        server.verify();
    }
}
