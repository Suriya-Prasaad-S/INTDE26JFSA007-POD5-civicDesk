package com.civicdesk.module.grievance.enums;

/**
 * Status of a {@code WORK} action, stored and exposed as short codes:
 * <pre>
 *   O  = Open
 *   IP = In Progress
 *   CM = Completed
 * </pre>
 * Only meaningful for {@code WORK} rows; {@code null} for system/workflow actions.
 */
public enum ActionStatus {
    O,
    IP,
    CM
}
