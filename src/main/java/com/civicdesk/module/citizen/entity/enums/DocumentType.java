package com.civicdesk.module.citizen.entity.enums;

/**
 * Allowed document types. Enforced at the API layer and at the DB layer
 * via a CHECK constraint.
 */
public enum DocumentType {
    NationalID,
    ResidenceProof,
    BirthCertificate,
    IncomeCertificate
}
