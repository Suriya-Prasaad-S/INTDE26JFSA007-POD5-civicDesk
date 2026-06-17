package com.civicdesk.module.auditlog.client;

import com.civicdesk.module.auditlog.dto.request.CreateAuditLogRequest;
import com.civicdesk.module.auditlog.enums.AuditAction;
import com.civicdesk.module.auditlog.enums.AuditModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client other modules/services use to write an audit entry by calling
 * {@code POST /audit/auditLogs} on the audit-log service.
 *
 * <p>In a single deployment this hits the same app; once the audit log is deployed as its
 * own service, only {@code app.audit.base-url} changes. Recording is <strong>best-effort</strong>:
 * a transport or HTTP error is logged and swallowed so auditing never fails the caller's
 * business operation (mirroring the fire-and-forget in-process {@code AuditService.log}).
 */
@Component
public class AuditClient {

    private static final Logger log = LoggerFactory.getLogger(AuditClient.class);
    private static final String AUDIT_LOGS_PATH = "/audit/auditLogs";

    private final RestClient restClient;

    public AuditClient(RestClient.Builder builder,
                       @Value("${app.audit.base-url:http://localhost:8081/civicDesk}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /** Enum-typed overload — preferred, so audit values stay canonical across modules. */
    public void record(String userId, AuditAction action, AuditModule module, String authorizationHeader) {
        record(userId, action.name(), module.name(), authorizationHeader);
    }

    /**
     * POSTs a single audit entry. The client IP is resolved server-side by the audit service,
     * so it is intentionally not sent here.
     *
     * @param authorizationHeader the caller's inbound {@code Authorization} header, forwarded
     *                            verbatim so the audit service authenticates the same principal.
     */
    public void record(String userId, String action, String module, String authorizationHeader) {
        CreateAuditLogRequest body = new CreateAuditLogRequest();
        body.setUserId(userId);
        body.setAction(action);
        body.setModule(module);
        try {
            restClient.post()
                    .uri(AUDIT_LOGS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Failed to record audit entry (userId={}, action={}, module={}): {}",
                    userId, action, module, ex.getMessage());
        }
    }
}
