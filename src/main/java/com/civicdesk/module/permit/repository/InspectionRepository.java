package com.civicdesk.module.permit.repository;

import com.civicdesk.module.permit.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, String> {

    List<Inspection> findByPermitId(String permitId);

    List<Inspection> findByAssignedOfficerId(String assignedOfficerId);

    Optional<Inspection> findByInspectionId(String inspectionId);

    @Query("""
SELECT i.status AS label,
       COUNT(i) AS count
FROM Inspection i
GROUP BY i.status
""")
    <T> List<T> getStatusBreakdown(Class<T> type);

    @Query("""
SELECT i.outcome AS label,
       COUNT(i) AS count
FROM Inspection i
WHERE i.outcome IS NOT NULL
GROUP BY i.outcome
""")
    <T> List<T> getOutcomeBreakdown(Class<T> type);
}