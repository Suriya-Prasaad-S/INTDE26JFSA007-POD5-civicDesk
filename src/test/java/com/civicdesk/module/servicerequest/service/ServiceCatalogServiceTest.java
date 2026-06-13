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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServiceCatalogService}. A real {@link ObjectMapper} is used so the
 * requiredDocuments JSON round-trip is exercised end to end; only the repositories are mocked.
 */
@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock private ServiceCatalogRepository catalogRepository;
    @Mock private DepartmentRepository departmentRepository;

    private ServiceCatalogService service;

    private Department department;

    @BeforeEach
    void setUp() {
        // ObjectMapper is a real collaborator (not a mock): toJson/fromJson are pure helpers.
        service = new ServiceCatalogService(catalogRepository, departmentRepository, new ObjectMapper());
        department = new Department("DEP-1", "Revenue", "revenue@city.gov");
    }

    @Nested
    @DisplayName("createService")
    class CreateService {

        @Test
        @DisplayName("creates an Active service, serialising requiredDocuments to JSON")
        void createsSuccessfully() {
            CreateServiceRequest req = new CreateServiceRequest(
                    "Birth Certificate", "DEP-1", ServiceCategory.Certificate, 7,
                    List.of("NationalID", "ResidenceProof"), new BigDecimal("50.00"));
            when(catalogRepository.existsByServiceNameIgnoreCase("Birth Certificate")).thenReturn(false);
            when(departmentRepository.findById("DEP-1")).thenReturn(Optional.of(department));

            MessageResponse response = service.createService(req);

            ArgumentCaptor<ServiceCatalog> captor = ArgumentCaptor.forClass(ServiceCatalog.class);
            verify(catalogRepository).save(captor.capture());
            ServiceCatalog saved = captor.getValue();

            assertThat(saved.getServiceId()).isNotBlank();
            assertThat(saved.getServiceName()).isEqualTo("Birth Certificate");
            assertThat(saved.getDepartment()).isSameAs(department);
            assertThat(saved.getCategory()).isEqualTo(ServiceCategory.Certificate);
            assertThat(saved.getProcessingDays()).isEqualTo(7);
            assertThat(saved.getFee()).isEqualByComparingTo("50.00");
            assertThat(saved.getStatus()).isEqualTo(ServiceStatus.Active);
            assertThat(saved.getRequiredDocuments()).isEqualTo("[\"NationalID\",\"ResidenceProof\"]");
            assertThat(response.message()).contains("created successfully");
        }

        @Test
        @DisplayName("defaults a null fee to zero and null documents to an empty JSON array")
        void defaultsFeeAndDocuments() {
            CreateServiceRequest req = new CreateServiceRequest(
                    "Water Connection", "DEP-1", ServiceCategory.Utility, 3, null, null);
            when(catalogRepository.existsByServiceNameIgnoreCase("Water Connection")).thenReturn(false);
            when(departmentRepository.findById("DEP-1")).thenReturn(Optional.of(department));

            service.createService(req);

            ArgumentCaptor<ServiceCatalog> captor = ArgumentCaptor.forClass(ServiceCatalog.class);
            verify(catalogRepository).save(captor.capture());
            assertThat(captor.getValue().getFee()).isEqualByComparingTo("0");
            assertThat(captor.getValue().getRequiredDocuments()).isEqualTo("[]");
        }

        @Test
        @DisplayName("rejects a duplicate service name")
        void duplicateName() {
            CreateServiceRequest req = new CreateServiceRequest(
                    "Birth Certificate", "DEP-1", ServiceCategory.Certificate, 7, List.of(), null);
            when(catalogRepository.existsByServiceNameIgnoreCase("Birth Certificate")).thenReturn(true);

            assertThatThrownBy(() -> service.createService(req))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already exists");

            verify(catalogRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the department does not exist")
        void departmentNotFound() {
            CreateServiceRequest req = new CreateServiceRequest(
                    "Birth Certificate", "DEP-X", ServiceCategory.Certificate, 7, List.of(), null);
            when(catalogRepository.existsByServiceNameIgnoreCase("Birth Certificate")).thenReturn(false);
            when(departmentRepository.findById("DEP-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createService(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("DEP-X");
        }
    }

    @Nested
    @DisplayName("getAllServices")
    class GetAllServices {

        @Test
        @DisplayName("no category -> only Active services")
        void noCategory() {
            when(catalogRepository.findByStatus(ServiceStatus.Active))
                    .thenReturn(List.of(sampleCatalog()));

            List<ServiceListItemResponse> result = service.getAllServices(null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).serviceName()).isEqualTo("Birth Certificate");
            verify(catalogRepository).findByStatus(ServiceStatus.Active);
        }

        @Test
        @DisplayName("with category -> Active services filtered by category")
        void withCategory() {
            when(catalogRepository.findByStatusAndCategory(ServiceStatus.Active, ServiceCategory.Certificate))
                    .thenReturn(List.of(sampleCatalog()));

            service.getAllServices(ServiceCategory.Certificate);

            verify(catalogRepository).findByStatusAndCategory(ServiceStatus.Active, ServiceCategory.Certificate);
        }
    }

    @Nested
    @DisplayName("getService")
    class GetService {

        @Test
        @DisplayName("returns detail and parses requiredDocuments JSON back into a list")
        void returnsDetail() {
            ServiceCatalog catalog = sampleCatalog();
            catalog.setRequiredDocuments("[\"NationalID\"]");
            when(catalogRepository.findById("SVC-1")).thenReturn(Optional.of(catalog));

            ServiceDetailResponse detail = service.getService("SVC-1");

            assertThat(detail.serviceId()).isEqualTo("SVC-1");
            assertThat(detail.requiredDocuments()).containsExactly("NationalID");
            assertThat(detail.status()).isEqualTo(ServiceStatus.Active);
        }

        @Test
        @DisplayName("treats blank stored JSON as an empty list")
        void blankDocuments() {
            ServiceCatalog catalog = sampleCatalog();
            catalog.setRequiredDocuments(null);
            when(catalogRepository.findById("SVC-1")).thenReturn(Optional.of(catalog));

            ServiceDetailResponse detail = service.getService("SVC-1");

            assertThat(detail.requiredDocuments()).isEmpty();
        }

        @Test
        @DisplayName("throws when the service does not exist")
        void notFound() {
            when(catalogRepository.findById("SVC-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getService("SVC-X"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateService")
    class UpdateService {

        @Test
        @DisplayName("updates editable fields and persists")
        void updates() {
            ServiceCatalog catalog = sampleCatalog();
            when(catalogRepository.findById("SVC-1")).thenReturn(Optional.of(catalog));

            MessageResponse response = service.updateService("SVC-1",
                    new UpdateServiceRequest("Birth Certificate (Express)", 2,
                            new BigDecimal("80.00"), ServiceStatus.Inactive));

            assertThat(catalog.getServiceName()).isEqualTo("Birth Certificate (Express)");
            assertThat(catalog.getProcessingDays()).isEqualTo(2);
            assertThat(catalog.getFee()).isEqualByComparingTo("80.00");
            assertThat(catalog.getStatus()).isEqualTo(ServiceStatus.Inactive);
            assertThat(response.message()).contains("updated successfully");
            verify(catalogRepository).save(catalog);
        }

        @Test
        @DisplayName("defaults a null fee to zero on update")
        void nullFeeDefaultsToZero() {
            ServiceCatalog catalog = sampleCatalog();
            when(catalogRepository.findById("SVC-1")).thenReturn(Optional.of(catalog));

            service.updateService("SVC-1",
                    new UpdateServiceRequest("Birth Certificate", 7, null, ServiceStatus.Active));

            assertThat(catalog.getFee()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("throws when the service does not exist")
        void notFound() {
            when(catalogRepository.findById("SVC-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateService("SVC-X",
                    new UpdateServiceRequest("x", 1, BigDecimal.ZERO, ServiceStatus.Active)))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(catalogRepository, never()).save(any());
        }
    }

    private ServiceCatalog sampleCatalog() {
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setServiceId("SVC-1");
        catalog.setServiceName("Birth Certificate");
        catalog.setDepartment(department);
        catalog.setCategory(ServiceCategory.Certificate);
        catalog.setProcessingDays(7);
        catalog.setFee(new BigDecimal("50.00"));
        catalog.setStatus(ServiceStatus.Active);
        catalog.setRequiredDocuments("[]");
        return catalog;
    }
}
