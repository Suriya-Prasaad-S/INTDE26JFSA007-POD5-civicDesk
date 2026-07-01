package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.ServiceRequest;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.external.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, String> {

    /**
     * Current workload of an officer: how many of their assigned requests are not in a
     * terminal state. Used by auto-assignment to pick the least-loaded officer.
     */
    long countByAssignedOfficerAndStatusNotIn(User assignedOfficer, Collection<RequestStatus> statuses);

    /** All requests in a given status (getAllRequests?status=...). */
    List<ServiceRequest> findByStatus(RequestStatus status);

    /** All requests whose service belongs to a department (getAllRequests?departmentId=...). */
    List<ServiceRequest> findByService_Department_DepartmentId(String departmentId);

    /** Both filters combined. */
    List<ServiceRequest> findByStatusAndService_Department_DepartmentId(RequestStatus status, String departmentId);

    /** All requests submitted by a citizen (getRequestsByCitizen). */
    List<ServiceRequest> findByCitizen_CitizenId(String citizenId);

    @Query("""
            SELECT COUNT(r)
            FROM ServiceRequest r
            WHERE (:departmentId IS NULL OR r.service.department.departmentId = :departmentId)
              AND (:fromDate IS NULL OR r.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR r.submissionDate <= :toDate)
            """)
    long countRequests(
            @Param("departmentId") String departmentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT r.status, COUNT(r)
            FROM ServiceRequest r
            WHERE (:departmentId IS NULL OR r.service.department.departmentId = :departmentId)
              AND (:fromDate IS NULL OR r.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR r.submissionDate <= :toDate)
            GROUP BY r.status
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> getStatusBreakdown(
            @Param("departmentId") String departmentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT r.service.serviceName, COUNT(r)
            FROM ServiceRequest r
            WHERE (:departmentId IS NULL OR r.service.department.departmentId = :departmentId)
              AND (:fromDate IS NULL OR r.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR r.submissionDate <= :toDate)
            GROUP BY r.service.serviceName
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> getServiceBreakdown(
            @Param("departmentId") String departmentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT r.submissionDate, COUNT(r)
            FROM ServiceRequest r
            WHERE (:departmentId IS NULL OR r.service.department.departmentId = :departmentId)
              AND (:fromDate IS NULL OR r.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR r.submissionDate <= :toDate)
            GROUP BY r.submissionDate
            ORDER BY r.submissionDate
            """)
    List<Object[]> getTrend(
            @Param("departmentId") String departmentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COUNT(r)
            FROM ServiceRequest r
            WHERE (:departmentId IS NULL OR r.service.department.departmentId = :departmentId)
              AND (:fromDate IS NULL OR r.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR r.submissionDate <= :toDate)
              AND r.status <> com.civicdesk.module.serviceRequest.entity.enums.RequestStatus.Completed
              AND r.expectedCompletionDate < :today
            """)
    long countOverdueRequests(
            @Param("departmentId") String departmentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("today") LocalDate today);
}
