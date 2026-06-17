package com.civicdesk.module.serviceRequest.exception;

import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.ConflictException;
import com.civicdesk.common.exception.ForbiddenException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.exception.UnprocessableEntityException;
import com.civicdesk.common.response.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Translates exceptions thrown by the Service Request module into the standard
 * {@link ApiError} body (a single {@code message}) with the correct HTTP status.
 *
 * <p>Scoped to the {@code serviceRequest} package so it owns the error contract for this
 * module's endpoints, while the IAM / Audit modules keep their own
 * ({@link com.civicdesk.common.exception.GlobalExceptionHandler}). Both envelopes expose a
 * {@code message} field, so callers see a consistent shape across modules.</p>
 */
@RestControllerAdvice(basePackages = "com.civicdesk.module.serviceRequest")
public class ServiceRequestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ApiError> handleUnprocessable(UnprocessableEntityException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Bean-validation failures on @Valid request bodies → 400 with the field messages folded in. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Set<String> messages = new LinkedHashSet<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            messages.add(fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed. " + String.join("; ", messages) + ".");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxSize(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.BAD_REQUEST, "Uploaded file exceeds the maximum allowed size");
    }

    /** Catch-all so unexpected failures still return the standard JSON shape, not a stack trace. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(message));
    }
}
