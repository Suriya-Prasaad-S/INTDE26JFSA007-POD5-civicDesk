package com.civicdesk.module.notification.entity.enums;

/**
 * Lifecycle state of a notification. Stored as a string column and mirrors the
 * {@code CHECK (status IN (...))} constraint on the {@code notification} table.
 * A freshly created notification starts as {@link #Unread}.
 */
public enum Status {
    Unread,
    Read,
    Dismissed
}
