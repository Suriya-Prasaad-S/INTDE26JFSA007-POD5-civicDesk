package com.civicdesk.module.grievance.enums;

/**
 * Current handling tier of a grievance (just the tier, not an escalate action):
 * <pre>
 *   L1 = field-officer tier (default at creation)
 *   L2 = supervisor tier (review / resolve / no-FO handling)
 *   L3 = reserved (future)
 * </pre>
 */
public enum EscalationLevel {
    L1,
    L2,
    L3
}
