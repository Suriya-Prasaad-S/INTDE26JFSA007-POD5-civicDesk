package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.external.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Read/seed access to the placeholder {@code departments} table. See
 * {@link Department} for the temporary-placeholder note.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
}
