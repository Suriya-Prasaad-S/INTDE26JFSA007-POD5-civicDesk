package com.civicdesk.module.publicworks.repository;

import com.civicdesk.module.publicworks.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, String> {

    
List<Milestone> findByWorkOrderIdAndIsDeletedFalse(String workOrderId);
Optional<Milestone> findByMilestoneIdAndIsDeletedFalse(String milestoneId);
List<Milestone> findByStatusAndIsDeletedFalse(String status);
List<Milestone> findByWorkOrderIdAndStatusAndIsDeletedFalse(String workOrderId, String status);
List<Milestone> findByIsDeletedFalse();   // ← this was missing

}
