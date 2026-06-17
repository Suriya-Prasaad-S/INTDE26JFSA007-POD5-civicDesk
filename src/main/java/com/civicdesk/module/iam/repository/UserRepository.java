package com.civicdesk.module.iam.repository;

import com.civicdesk.module.iam.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByDepartmentId(String departmentId, Pageable pageable);

    Page<User> findByDepartmentIdAndStatus(String departmentId, String status, Pageable pageable);

    Page<User> findByRole(String role, Pageable pageable);

    /**
     * Active users of a given role in a department. Used by the Service Request module's
     * officer auto-assignment to pick the least-loaded officer (role {@code FO}, status {@code A}).
     */
    List<User> findByRoleAndStatusAndDepartmentId(String role, String status, String departmentId);
}
