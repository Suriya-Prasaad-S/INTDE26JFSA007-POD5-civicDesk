package com.civicdesk.module.serviceRequest.security;

import com.civicdesk.common.exception.ForbiddenException;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.serviceRequest.repository.CitizenProfileRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceRequestRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Role / ownership rules for the Service Request endpoints, matching the module's API access
 * matrix.
 *
 * <p>Authentication itself is enforced by Spring Security — every {@code /serviceRequest/**}
 * path requires a valid JWT (a missing/invalid token yields 401). This guard layers the
 * per-endpoint <b>authorization</b> on top, throwing {@link ForbiddenException} (HTTP 403)
 * with an endpoint-specific message when the caller's role is not permitted or they are
 * acting on a resource they do not own.</p>
 *
 * <p>Role mapping: {@code CIT}=Citizen, {@code FO}=Field Officer, {@code DS}=Department
 * Supervisor (together the "Department Officer" category), {@code ADM}=Admin.</p>
 */
@Component
public class ServiceRequestAccessGuard {

    private static final String CITIZEN = "CIT";
    private static final String FIELD_OFFICER = "FO";
    private static final String SUPERVISOR = "DS";
    private static final String ADMIN = "ADM";

    /** Who may read the request queue / any single request / its documents. */
    private static final Set<String> REQUEST_VIEWERS = Set.of(FIELD_OFFICER, SUPERVISOR, ADMIN);
    /** Who may act on requests/documents (update status, verify): officers and supervisors. */
    private static final Set<String> OFFICERS = Set.of(FIELD_OFFICER, SUPERVISOR);

    private final ServiceRequestRepository requestRepository;
    private final CitizenProfileRepository citizenProfileRepository;

    public ServiceRequestAccessGuard(ServiceRequestRepository requestRepository,
                                     CitizenProfileRepository citizenProfileRepository) {
        this.requestRepository = requestRepository;
        this.citizenProfileRepository = citizenProfileRepository;
    }

    // ------------------------------------------------------------------ Catalog services

    /** createService — Admin only. */
    public void canCreateService() {
        if (!isAdmin()) {
            throw new ForbiddenException("Access denied. Only Admin role can create services.");
        }
    }

    /** updateService — Admin only. */
    public void canUpdateService() {
        if (!isAdmin()) {
            throw new ForbiddenException("Access denied. Only Admin role can update services.");
        }
    }

    // ------------------------------------------------------------------ Service requests

    /** submitRequest — Citizen only. */
    public void canSubmitRequest() {
        if (!isCitizen()) {
            throw new ForbiddenException("Access denied. Only citizens can submit service requests.");
        }
    }

    /** getAllRequests — Department Officer / Supervisor / Admin (citizens denied). */
    public void canViewRequestQueue() {
        if (!REQUEST_VIEWERS.contains(role())) {
            throw new ForbiddenException("Access denied. Citizens cannot access the request queue.");
        }
    }

    /** getRequest — staff (FO/DS/ADM) may view any; a citizen may view only their own. */
    public void canViewRequest(String requestId) {
        if (REQUEST_VIEWERS.contains(role())) {
            return;
        }
        if (isCitizen() && ownsRequest(requestId)) {
            return;
        }
        throw new ForbiddenException("Access denied. You are not authorized to view this request.");
    }

    /** getRequestsByCitizen — a citizen may list only their own requests. */
    public void canViewCitizenRequests(String citizenId) {
        if (isCitizen() && citizenId != null && citizenId.equals(callerCitizenId())) {
            return;
        }
        throw new ForbiddenException("Access denied. You can only view your own requests.");
    }

    /** updateRequestStatus — Department Officer / Supervisor only. */
    public void canUpdateRequestStatus() {
        if (!OFFICERS.contains(role())) {
            throw new ForbiddenException(
                    "Access denied. Only Officer or Supervisor role can update request status.");
        }
    }

    // ------------------------------------------------------------------ Documents

    /** uploadDocument — a citizen may upload only to their own request. */
    public void canUploadDocument(String requestId) {
        if (isCitizen() && ownsRequest(requestId)) {
            return;
        }
        throw new ForbiddenException(
                "Access denied. You can only upload documents to your own requests.");
    }

    /** getDocuments — staff (FO/DS/ADM) may view any; a citizen only for their own request. */
    public void canViewDocuments(String requestId) {
        if (REQUEST_VIEWERS.contains(role())) {
            return;
        }
        if (isCitizen() && ownsRequest(requestId)) {
            return;
        }
        throw new ForbiddenException("Access denied. You are not authorized to view these documents.");
    }

    /** verifyDocument — Department Officer / Supervisor only. */
    public void canVerifyDocument() {
        if (!OFFICERS.contains(role())) {
            throw new ForbiddenException(
                    "Access denied. Only Officer or Supervisor role can verify documents.");
        }
    }

    // ------------------------------------------------------------------ Helpers

    private String role() {
        return SecurityContextUtil.getCurrentRole();
    }

    private boolean isAdmin() {
        return ADMIN.equals(role());
    }

    private boolean isCitizen() {
        return CITIZEN.equals(role());
    }

    /** The citizenId owned by the current principal, or {@code null} if the user has no profile. */
    private String callerCitizenId() {
        String userId = SecurityContextUtil.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return citizenProfileRepository.findByUserId(userId)
                .map(cp -> cp.getCitizenId())
                .orElse(null);
    }

    /** True when the given request exists and belongs to the current citizen principal. */
    private boolean ownsRequest(String requestId) {
        String callerCitizenId = callerCitizenId();
        if (callerCitizenId == null) {
            return false;
        }
        return requestRepository.findById(requestId)
                .map(r -> callerCitizenId.equals(r.getCitizen().getCitizenId()))
                .orElse(false);
    }
}
