package com.civicdesk.module.notification.dto.response;

/**
 * Minimal response carrying only a human-readable message, for endpoints where the
 * client just needs confirmation text (mark as read, dismiss, delete).
 */
public record MessageResponse(String message) {
}
