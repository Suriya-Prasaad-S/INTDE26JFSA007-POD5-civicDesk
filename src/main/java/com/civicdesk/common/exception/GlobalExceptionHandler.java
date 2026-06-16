package com.civicdesk.common.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.civicdesk.common.exception.grievance.ActionNotEditableException;
import com.civicdesk.common.exception.grievance.ActionNotFoundException;
import com.civicdesk.common.exception.grievance.GrievanceActionCreationException;
import com.civicdesk.common.exception.grievance.GrievanceCreationException;
import com.civicdesk.common.exception.grievance.GrievanceNotFoundException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceDataException;
import com.civicdesk.common.exception.grievance.InvalidGrievanceStateException;
import com.civicdesk.common.exception.grievance.InvalidUserRoleException;
import com.civicdesk.common.exception.grievance.UnauthorizedGrievanceAccessException;
import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.response.ErrorResponse;

/**
 * Central exception handler. Every controller in the application routes its
 * failures through here so that clients always receive a consistent error body.
 *
 * <p>Grievance-module failures are returned as {@link ErrorResponse}; IAM-module
 * failures are returned as {@link ApiResponse}. Unifying these two error shapes
 * is a follow-up the team should align on.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Grievance module ---

    @ExceptionHandler(GrievanceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGrievanceNotFound(
            GrievanceNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidGrievanceDataException.class, InvalidUserRoleException.class})
    public ResponseEntity<ErrorResponse> handleGrievanceBadRequest(
            RuntimeException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({GrievanceCreationException.class, GrievanceActionCreationException.class})
    public ResponseEntity<ErrorResponse> handleGrievancePersistence(
            RuntimeException ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedGrievanceAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedGrievanceAccess(
            UnauthorizedGrievanceAccessException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidGrievanceStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGrievanceState(
            InvalidGrievanceStateException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ActionNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleActionNotEditable(
            ActionNotEditableException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ActionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleActionNotFound(
            ActionNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        // `message` joins all field errors so IAM-style callers reading $.message always
        // find the relevant text — even when one field trips several validators (e.g. a
        // blank phone fails both @NotBlank and @Pattern). The frontend uses the structured
        // `details` list to show errors per field.
        String message = details.isEmpty()
                ? "Validation failed for the submitted request"
                : String.join(", ", details);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(extractPath(request))
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** Catch-all so that no unexpected error ever leaks a raw stack trace to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request);
    }

    // --- IAM module ---

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse> handleTokenExpired(TokenExpiredException e) {
        return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException e) {
        // Raised by @PreAuthorize when a caller's role is not permitted on an endpoint.
        return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
    }

    @ExceptionHandler(PasswordNotSetException.class)
    public ResponseEntity<ApiResponse> handlePasswordNotSet(PasswordNotSetException e) {
        // Account exists but the owner hasn't set a password yet.
        return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse> handleBadRequest(BadRequestException e) {
        return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<ApiResponse> handleSuspended(AccountSuspendedException e) {
        // 423 Locked — the account exists and credentials are valid, but it is suspended.
        return ResponseEntity.status(423).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ApiResponse> handleInactive(AccountInactiveException e) {
        // 403 Forbidden — credentials are valid but the account is deactivated (neutral lifecycle state).
        return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse> handleDuplicate(DuplicateEmailException e) {
        return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
    }

    // --- Helpers (grievance ErrorResponse builder) ---

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(extractPath(request))
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private String extractPath(WebRequest request) {
        // WebRequest description is in the form "uri=/civicDesk/grievance/createGrievance"
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
