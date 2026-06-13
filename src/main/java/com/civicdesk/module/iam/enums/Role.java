package com.civicdesk.module.iam.enums;

/**
 * Canonical role names for CivicDesk. Shared with ALL teammates â€” the exact
 * string values here must match every {@code @PreAuthorize} guard and every
 * value persisted in {@code users.role}, or RBAC checks will silently fail.
 */


public enum Role {
    // Citizen 
    CIT,

    // Field Officer
    FO,

    // Department Supervisor
    DS,

    // Engineer
    ENG,

    // Complience Officer  
    CO,
    
    // Admin
    ADM
}
