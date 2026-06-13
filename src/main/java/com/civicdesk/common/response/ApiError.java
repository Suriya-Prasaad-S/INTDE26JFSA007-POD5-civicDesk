package com.civicdesk.common.response;

/**
 * Standard error response body. Carries only a human-readable {@code message}:
 * <pre>{ "message": "..." }</pre>
 * The HTTP status code is conveyed by the response status line itself, not duplicated here.
 */
public class ApiError {

    private final String message;

    public ApiError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
