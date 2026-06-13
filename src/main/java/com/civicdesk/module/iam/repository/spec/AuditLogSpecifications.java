package com.civicdesk.module.iam.repository.spec;

import com.civicdesk.module.iam.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable predicates for querying {@link AuditLog} by userId, action or
 * module. Callers {@code .and(...)} only the filters that were supplied, so any
 * combination of the three optional query params builds a single backing query —
 * served by the indexes declared on {@code AuditLog}.
 *
 * <p>action/module are stored as upper-case enum names, so those filters are
 * normalised to upper case; userId is matched verbatim.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> hasUserId(String userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, cb) -> cb.equal(root.get("action"), action.trim().toUpperCase());
    }

    public static Specification<AuditLog> hasModule(String module) {
        return (root, query, cb) -> cb.equal(root.get("module"), module.trim().toUpperCase());
    }
}
