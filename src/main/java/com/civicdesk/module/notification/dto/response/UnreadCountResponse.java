package com.civicdesk.module.notification.dto.response;

/**
 * Response for fetchUnreadCount: the number of Unread notifications a user currently has,
 * suitable for driving a UI badge.
 */
public record UnreadCountResponse(String userId, long unreadCount) {
}
