package com.civicdesk.module.notification.entity.enums;

/**
 * Domain a notification belongs to. Stored as a string column and mirrors the
 * {@code CHECK (category IN (...))} constraint on the {@code notification} table.
 */
public enum Category {
    ServiceRequest,
    Permit,
    Grievance,
    WorkOrder,
    Compliance
}
