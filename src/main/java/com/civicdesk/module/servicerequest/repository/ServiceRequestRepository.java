package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.ServiceRequest;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.external.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
