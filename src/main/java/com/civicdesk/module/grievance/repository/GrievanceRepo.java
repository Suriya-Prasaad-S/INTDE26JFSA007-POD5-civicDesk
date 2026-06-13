package com.civicdesk.module.grievance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.civicdesk.module.grievance.entity.Grievance;

@Repository
public interface GrievanceRepo extends JpaRepository<Grievance, String> {

    /** Grievances raised by a given citizen. */
    List<Grievance> findByCitizenId(String citizenId);

    /** Grievances assigned to a given user (e.g. a field officer). */
    List<Grievance> findByAssignedToId(String assignedToId);

    /** Grievances belonging to a department (the supervisor's queue). */
    List<Grievance> findByDepartmentId(String departmentId);
}
