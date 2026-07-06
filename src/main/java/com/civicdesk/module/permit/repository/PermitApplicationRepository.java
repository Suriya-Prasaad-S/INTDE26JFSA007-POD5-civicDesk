package com.civicdesk.module.permit.repository;

import com.civicdesk.module.permit.entity.PermitApplication;
import com.civicdesk.module.permit.enums.PermitStatus;
import com.civicdesk.module.permit.enums.PermitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PermitApplicationRepository extends JpaRepository<PermitApplication, String> {

    Optional<PermitApplication> findByPermitIdAndIsDeletedFalse(String permitId);

    List<PermitApplication> findByCitizenIdAndIsDeletedFalse(String citizenId);

    List<PermitApplication> findByIsDeletedFalse();

    List<PermitApplication> findByStatusAndIsDeletedFalse(PermitStatus status);

    List<PermitApplication> findByPermitTypeAndIsDeletedFalse(PermitType permitType);

    List<PermitApplication> findByStatusAndPermitTypeAndIsDeletedFalse(
            PermitStatus status, PermitType permitType);


    @Query("""
    SELECT COUNT(p)
    FROM PermitApplication p
    WHERE p.isDeleted = false
    AND p.applicationDate >= :from
    AND p.applicationDate <= :to
""")
    long countPermits(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
    SELECT p.status AS label,
           COUNT(p) AS count
    FROM PermitApplication p
    WHERE p.isDeleted = false
    AND p.applicationDate >= :from
    AND p.applicationDate <= :to
    GROUP BY p.status
""")
    <T> List<T> getStatusBreakdown(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Class<T> type
    );

    @Query("""
    SELECT p.permitType AS label,
           COUNT(p) AS count
    FROM PermitApplication p
    WHERE p.isDeleted = false
    AND p.applicationDate >= :from
    AND p.applicationDate <= :to
    GROUP BY p.permitType
""")
    <T> List<T> getPermitTypeBreakdown(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Class<T> type
    );

    @Query("""
    SELECT p.applicationDate AS date,
           COUNT(p) AS count
    FROM PermitApplication p
    WHERE p.isDeleted = false
    AND p.applicationDate >= :from
    AND p.applicationDate <= :to
    GROUP BY p.applicationDate
    ORDER BY p.applicationDate
""")
    <T> List<T> getApplicationTrend(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Class<T> type
    );

    @Query("""
    SELECT p.decisionDate AS date,
           COUNT(p) AS count
    FROM PermitApplication p
    WHERE p.isDeleted = false
    AND p.decisionDate IS NOT NULL
    AND p.decisionDate >= :from
    AND p.decisionDate <= :to
    GROUP BY p.decisionDate
    ORDER BY p.decisionDate
""")
    <T> List<T> getDecisionTrend(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Class<T> type
    );

    @Query("""
    SELECT p
    FROM PermitApplication p
    WHERE p.isDeleted = false
    AND p.decisionDate IS NOT NULL
    AND p.applicationDate >= :from
    AND p.applicationDate <= :to
""")
    List<PermitApplication> getDecidedPermits(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}