package com.civicdesk.module.citizen.controller;

import com.civicdesk.module.citizen.exception.BusinessRuleException;
import com.civicdesk.module.citizen.exception.DuplicateResourceException;
import com.civicdesk.module.citizen.exception.ForbiddenActionException;
import com.civicdesk.module.citizen.exception.InvalidRequestException;
import com.civicdesk.module.citizen.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps the citizen module's exceptions to HTTP responses for this module's controllers only.
 *
 * <p>The error body is intentionally minimal — just {@code {"message": "…"}} (no timestamp/status/
 * path). Scoped via {@code basePackages} to the citizen controllers so it cannot affect other
 * modules; remove it once the shared {@code common/} GlobalExceptionHandler lands.
 */
@RestControllerAdvice(basePackages = "com.civicdesk.module.citizen.controller")
public class CitizenExceptionHandler {

    /** 404 — citizen, document or file not found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** 409 — duplicate email / national ID at registration. */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** 409 — illegal status transition or a violated business rule (e.g. document limit). */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** 403 — caller not permitted (e.g. verifier is not a Department Supervisor). */
    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenActionException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** 400 — semantically invalid request (bad enum/code, empty update, invalid/oversized file). */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** 400 — Bean Validation failures on a {@code @Valid @RequestBody}, collapsed to one message. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Validation failed" : message);
    }

    /** 400 — required multipart part missing (e.g. the {@code file} part on upload). */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingPart(MissingServletRequestPartException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** 400 — required request parameter missing (e.g. {@code documentType} on upload). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** 400 — malformed / unreadable JSON body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    /** 413 — upload exceeds the servlet multipart cap. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large");
    }

    private static ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
