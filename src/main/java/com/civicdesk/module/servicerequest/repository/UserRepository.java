package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.external.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read/seed access to the placeholder {@code users} table. See {@link User} for the
 * temporary-placeholder note.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /** Active officers in a department, used by auto-assignment to pick the least loaded one. */
    List<User> findByRoleAndStatusAndDepartmentId(String role, String status, String departmentId);
}
