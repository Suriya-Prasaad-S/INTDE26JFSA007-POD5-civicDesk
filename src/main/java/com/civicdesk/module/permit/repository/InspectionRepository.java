package com.civicdesk.module.permit.repository;

import com.civicdesk.module.permit.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, String> {

    List<Inspection> findByPermitId(String permitId);

    List<Inspection> findByAssignedOfficerId(String assignedOfficerId);

    Optional<Inspection> findByInspectionId(String inspectionId);
}