package com.civicdesk.module.grievance.enums;

/**
 * Fixed list of grievance categories, stored and exposed as short codes.
 * Each category routes to exactly one department (resolved to a {@code departmentId}
 * via the IAM {@code departments} table at create time).
 *
 * <pre>
 *   RI = Road infrastructure   → Infrastructure
 *   WS = Water supply          → Public Health
 *   SN = Sanitation            → Public Health
 *   SD = Service delay         → Compliance & Audit
 *   CR = Corruption            → Compliance & Audit
 *   OT = Other                 → Administration
 * </pre>
 */
public enum Category {

    RI("Infrastructure"),
    WS("Public Health"),
    SN("Public Health"),
    SD("Compliance & Audit"),
    CR("Compliance & Audit"),
    OT("Administration");

    private final String departmentName;

    Category(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentName() {
        return departmentName;
    }
}
