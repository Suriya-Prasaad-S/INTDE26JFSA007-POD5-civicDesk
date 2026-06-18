package com.civicdesk.common.exception;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.civicdesk.common.exception.citizen.BusinessRuleException;
import com.civicdesk.common.exception.citizen.DuplicateResourceException;
import com.civicdesk.common.exception.citizen.ForbiddenActionException;
import com.civicdesk.common.exception.citizen.InvalidRequestException;
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

/**
 * Central exception handler. Every controller in the application routes its failures through here,
 * and they all return the shared {@link ApiResponse} envelope ({@code {message, data}}) — the IAM
 * convention, now applied to the grievance and citizen modules too.
 *
 * <p>Validation failures return the <b>first</b> field error in {@code message} (presence
 * constraints such as {@code @NotBlank}/{@code @NotNull} take precedence over format constraints
 * on the same field).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Grievance module ---

    @ExceptionHandler(GrievanceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleGrievanceNotFound(GrievanceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({InvalidGrievanceDataException.class, InvalidUserRoleException.class})
    public ResponseEntity<ApiResponse> handleGrievanceBadRequest(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({GrievanceCreationException.class, GrievanceActionCreationException.class})
    public ResponseEntity<ApiResponse> handleGrievancePersistence(RuntimeException ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedGrievanceAccessException.class)
    public ResponseEntity<ApiResponse> handleUnauthorizedGrievanceAccess(
            UnauthorizedGrievanceAccessException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidGrievanceStateException.class)
    public ResponseEntity<ApiResponse> handleInvalidGrievanceState(InvalidGrievanceStateException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ActionNotEditableException.class)
    public ResponseEntity<ApiResponse> handleActionNotEditable(ActionNotEditableException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ActionNotFoundException.class)
    public ResponseEntity<ApiResponse> handleActionNotFound(ActionNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // --- Citizen module ---

    // FQN on the type: this package already has an (IAM) ResourceNotFoundException, so the citizen
    // one is referenced fully-qualified to avoid the name clash.
    @ExceptionHandler(com.civicdesk.common.exception.citizen.ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleCitizenNotFound(
            com.civicdesk.common.exception.citizen.ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({DuplicateResourceException.class, BusinessRuleException.class})
    public ResponseEntity<ApiResponse> handleCitizenConflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ApiResponse> handleForbiddenAction(ForbiddenActionException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse> handleInvalidRequest(InvalidRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // --- Request/multipart failures (used by the citizen document upload) ---

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleTooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large");
    }

    @ExceptionHandler({MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse> handleMissingPartOrParam(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    // @RequestBody validation (JSON bodies).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        return firstFieldError(ex.getBindingResult().getFieldErrors());
    }

    // @ModelAttribute validation (multipart/form-data binding, e.g. citizen registration).
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse> handleBind(BindException ex) {
        return firstFieldError(ex.getFieldErrors());
    }

    /** Catch-all so that no unexpected error ever leaks a raw stack trace to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
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

    // --- Helpers ---

    private static ResponseEntity<ApiResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }

    // Return only the FIRST field error. Presence constraints (@NotBlank/@NotNull/@NotEmpty) rank
    // ahead of format constraints so a blank field reports "x is required" rather than a format
    // message when one field trips several validators.
    private static ResponseEntity<ApiResponse> firstFieldError(List<FieldError> fieldErrors) {
        String message = fieldErrors.stream()
                .min(Comparator.comparingInt(GlobalExceptionHandler::constraintRank))
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed for the submitted request");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /** Presence constraints rank first (0) so "x is required" wins over format messages (1). */
    private static int constraintRank(FieldError fieldError) {
        String code = fieldError.getCode();
        if (code != null
                && (code.startsWith("NotBlank") || code.startsWith("NotNull") || code.startsWith("NotEmpty"))) {
            return 0;
        }
        return 1;
    }
}
