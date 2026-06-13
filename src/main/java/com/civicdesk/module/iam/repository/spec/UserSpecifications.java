package com.civicdesk.module.iam.repository.spec;

import com.civicdesk.module.iam.entity.User;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable predicates for querying {@link User}. Each factory returns a
 * single-column filter; callers {@code .and(...)} them together to build the final query,
 * which keeps the optional role/status/department combinations from exploding into a
 * separate repository method per case.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasRole(String role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<User> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<User> inDepartment(String departmentId) {
        return (root, query, cb) -> cb.equal(root.get("departmentId"), departmentId);
    }
}
