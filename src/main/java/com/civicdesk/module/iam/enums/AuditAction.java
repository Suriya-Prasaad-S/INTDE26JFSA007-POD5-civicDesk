package com.civicdesk.module.iam.enums;

/** Actions recorded in the audit log by the IAM module. */
public enum AuditAction {
    REGISTER,
    LOGIN,
    LOGOUT,
    CREATE_USER,
    UPDATE_STATUS,
    SET_PASSWORD,
    SEED_ADMIN
}
