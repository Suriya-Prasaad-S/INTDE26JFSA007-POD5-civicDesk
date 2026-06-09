package com.civicdesk.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard response envelope. Null fields are omitted, so:
 * <ul>
 *   <li>GET success → {@code { "data": ... }} (no message)</li>
 *   <li>POST/PUT success → {@code { "message": "...", "data": ... }} (data omitted when null)</li>
 *   <li>Error → {@code { "message": "..." }}</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    private String message;
    private Object data;

    public ApiResponse(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    /** GET success: data only, no message. */
    public static ApiResponse data(Object data) {
        return new ApiResponse(null, data);
    }

    /** POST/PUT success: message plus optional data. */
    public static ApiResponse of(String message, Object data) {
        return new ApiResponse(message, data);
    }

    /** Error: message only. */
    public static ApiResponse error(String message) {
        return new ApiResponse(message, null);
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
