package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.ConflictException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.module.serviceRequest.dto.request.CreateServiceRequest;
import com.civicdesk.module.serviceRequest.dto.request.UpdateServiceRequest;
import com.civicdesk.module.serviceRequest.dto.response.MessageResponse;
import com.civicdesk.module.serviceRequest.dto.response.ServiceDetailResponse;
import com.civicdesk.module.serviceRequest.dto.response.ServiceListItemResponse;
import com.civicdesk.module.serviceRequest.entity.ServiceCatalog;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.external.Department;
import com.civicdesk.module.serviceRequest.repository.DepartmentRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceCatalogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for the service catalog. Role enforcement (Admin only for create/update)
 * is deferred until the IAM module wires up authentication.
 */
@Service
public class ServiceCatalogService {

    private final ServiceCatalogRepository catalogRepository;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;

    public ServiceCatalogService(ServiceCatalogRepository catalogRepository,
                                 DepartmentRepository departmentRepository,
                                 ObjectMapper objectMapper) {
        this.catalogRepository = catalogRepository;
        this.departmentRepository = departmentRepository;
        this.objectMapper = objectMapper;
    }

    public MessageResponse createService(CreateServiceRequest request) {
        if (catalogRepository.existsByServiceNameIgnoreCase(request.serviceName())) {
            throw new ConflictException(
                    "A service named '" + request.serviceName() + "' already exists in the catalog.");
        }

        // The service must belong to a real department (FK service_catalog.departmentId).
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department with ID " + request.departmentId() + " does not exist"));

        ServiceCatalog service = new ServiceCatalog();
        service.setServiceId(UUID.randomUUID().toString());
        service.setServiceName(request.serviceName());
        service.setDepartment(department);
        service.setCategory(request.category());
        service.setProcessingDays(request.processingDays());
        service.setRequiredDocuments(toJson(request.requiredDocuments()));
        service.setFee(request.fee() == null ? BigDecimal.ZERO : request.fee());
        service.setStatus(ServiceStatus.Active);

        catalogRepository.save(service);

        return new MessageResponse(
                "Service created successfully. New service has been added to the catalog.");
    }

    /** All Active services, optionally filtered by category (getAllServices). */
    public List<ServiceListItemResponse> getAllServices(ServiceCategory category) {
        List<ServiceCatalog> services = (category == null)
                ? catalogRepository.findByStatus(ServiceStatus.Active)
                : catalogRepository.findByStatusAndCategory(ServiceStatus.Active, category);

        return services.stream().map(this::toListItem).toList();
    }

    /** Full details of one service (getService). */
    public ServiceDetailResponse getService(String serviceId) {
        ServiceCatalog service = catalogRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found. No service exists with the given serviceId."));
        return toDetail(service);
    }

    /** Update the editable fields of a service, or deactivate it (updateService). */
    public MessageResponse updateService(String serviceId, UpdateServiceRequest request) {
        ServiceCatalog service = catalogRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found. No service exists with the given serviceId."));

        service.setServiceName(request.serviceName());
        service.setProcessingDays(request.processingDays());
        service.setFee(request.fee() == null ? BigDecimal.ZERO : request.fee());
        service.setStatus(request.status());

        catalogRepository.save(service);

        return new MessageResponse("Service updated successfully.");
    }

    private ServiceListItemResponse toListItem(ServiceCatalog s) {
        return new ServiceListItemResponse(s.getServiceId(), s.getServiceName(),
                s.getDepartment().getDepartmentId(), s.getCategory(), s.getProcessingDays(),
                s.getFee(), s.getStatus());
    }

    private ServiceDetailResponse toDetail(ServiceCatalog s) {
        return new ServiceDetailResponse(s.getServiceId(), s.getServiceName(),
                s.getDepartment().getDepartmentId(), s.getCategory(), s.getProcessingDays(),
                fromJson(s.getRequiredDocuments()), s.getFee(), s.getStatus());
    }

    private String toJson(List<String> requiredDocuments) {
        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(requiredDocuments);
        } catch (JsonProcessingException ex) {
            // List<String> is always serializable; treat as a programming error.
            throw new IllegalStateException("Failed to serialize requiredDocuments", ex);
        }
    }

    private List<String> fromJson(String requiredDocumentsJson) {
        if (!StringUtils.hasText(requiredDocumentsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(requiredDocumentsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            // Stored by toJson(), so this should never fail; treat as a programming error.
            throw new IllegalStateException("Failed to parse requiredDocuments", ex);
        }
    }
}
