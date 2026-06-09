package com.civicdesk.module.grievance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.civicdesk.module.grievance.entity.GrievanceAction;

@Repository
public interface GrievanceActionRepo extends JpaRepository<GrievanceAction, String> {

    /** All actions taken against a grievance, oldest first. */
    List<GrievanceAction> findByGrievanceIdOrderByActionDateAsc(String grievanceId);
}
