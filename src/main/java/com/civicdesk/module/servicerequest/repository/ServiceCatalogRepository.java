package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.ServiceCatalog;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, String> {

    /** Used to prevent creating a catalog service whose name already exists. */
    boolean existsByServiceNameIgnoreCase(String serviceName);

    /** All services in a given status (e.g. the Active services shown by getAllServices). */
    List<ServiceCatalog> findByStatus(ServiceStatus status);

    /** Services in a given status filtered by category (getAllServices?category=...). */
    List<ServiceCatalog> findByStatusAndCategory(ServiceStatus status, ServiceCategory category);
}
