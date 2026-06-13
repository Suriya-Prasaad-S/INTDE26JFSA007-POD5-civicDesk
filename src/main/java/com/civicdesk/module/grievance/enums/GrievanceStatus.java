package com.civicdesk.module.grievance.enums;

/**
 * Lifecycle status of a grievance, stored and exposed as short codes:
 * <pre>
 *   O  = Open
 *   IP = In Progress
 *   R  = Resolved
 *   C  = Closed
 *   RO = Reopened
 * </pre>
 * Flow: O → IP → R → C / RO.
 */
public enum GrievanceStatus {
    O,
    IP,
    R,
    C,
    RO
}
