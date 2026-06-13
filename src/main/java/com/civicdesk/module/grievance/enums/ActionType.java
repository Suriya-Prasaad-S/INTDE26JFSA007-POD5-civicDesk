package com.civicdesk.module.grievance.enums;

/**
 * Kind of timeline entry in {@code grievance_actions}, stored and exposed as short codes:
 * <pre>
 *   WK = WORK        — manual work entry (officer / supervisor); editable while not Completed
 *   AS = ASSIGNMENT  — supervisor assigned / reassigned a field officer
 *   RV = REVIEW      — field officer sent work for supervisor review
 *   RS = RESOLVE     — supervisor marked resolved
 *   CL = CLOSE       — citizen closed
 *   RP = REOPEN      — citizen reopened
 * </pre>
 */
public enum ActionType {
    WK,
    AS,
    RV,
    RS,
    CL,
    RP
}
