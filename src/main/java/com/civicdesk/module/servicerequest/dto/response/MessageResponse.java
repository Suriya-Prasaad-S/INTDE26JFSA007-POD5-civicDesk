package com.civicdesk.module.serviceRequest.dto.response;

/**
 * Minimal response carrying only a human-readable message, for endpoints where the
 * client just needs confirmation text (e.g. document upload).
 */
public record MessageResponse(String message) {
}
