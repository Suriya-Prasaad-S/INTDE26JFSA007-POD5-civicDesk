package com.civicdesk.module.serviceRequest.controller;

import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.exception.UnprocessableEntityException;
import com.civicdesk.module.serviceRequest.dto.response.MessageResponse;
import com.civicdesk.module.serviceRequest.dto.response.RequestListItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.ServiceDetailResponse;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.iam.security.JwtAuthFilter;
import com.civicdesk.module.serviceRequest.security.ServiceRequestAccessGuard;
import com.civicdesk.module.serviceRequest.service.DocumentService;
import com.civicdesk.module.serviceRequest.service.ServiceCatalogService;
import com.civicdesk.module.serviceRequest.service.ServiceRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ServiceRequestController}: request/response wiring, status
 * codes, bean-validation failures, the enum query-param converter, and the {@code ApiError}
 * mapping done by {@code GlobalExceptionHandler}. Services are mocked.
 */
@WebMvcTest(ServiceRequestController.class)
@AutoConfigureMockMvc(addFilters = false) // IAM security is on the classpath; disable filters for this slice.
class ServiceRequestControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ServiceCatalogService serviceCatalogService;
    @MockitoBean private ServiceRequestService serviceRequestService;
    @MockitoBean private DocumentService documentService;
    // IAM's SecurityConfig is pulled into the slice and needs this bean; mock it (filters disabled anyway).
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    // Authorization is verified end-to-end in the integration test; here the guard is a permissive
    // mock (its void checks do nothing) so these web-layer tests stay focused on request/response wiring.
    @MockitoBean private ServiceRequestAccessGuard accessGuard;

    // ---------------------------------------------------------------- Catalog services

    @Test
    @DisplayName("GET getService returns 200 with the service detail")
    void getServiceOk() throws Exception {
        when(serviceCatalogService.getService("SVC-1")).thenReturn(new ServiceDetailResponse(
                "SVC-1", "Birth Certificate", "DEP-1", ServiceCategory.Certificate, 7,
                List.of("NationalID"), new BigDecimal("50.00"), ServiceStatus.Active));

        mockMvc.perform(get("/serviceRequest/getService/SVC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value("SVC-1"))
                .andExpect(jsonPath("$.status").value("A"));
    }

    @Test
    @DisplayName("GET getService maps ResourceNotFoundException to 404")
    void getServiceNotFound() throws Exception {
        when(serviceCatalogService.getService("SVC-X"))
                .thenThrow(new ResourceNotFoundException("Service not found."));

        mockMvc.perform(get("/serviceRequest/getService/SVC-X"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Service not found."));
    }

    @Test
    @DisplayName("POST createService returns 201 for a valid body")
    void createServiceCreated() throws Exception {
        when(serviceCatalogService.createService(any()))
                .thenReturn(new MessageResponse("Service created successfully."));

        String body = """
                {"serviceName":"Birth Certificate","departmentId":"DEP-1",
                 "category":"Certificate","processingDays":7,
                 "requiredDocuments":["NationalID"],"fee":50.00}
                """;

        mockMvc.perform(post("/serviceRequest/createService")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Service created successfully."));
    }

    @Test
    @DisplayName("POST createService returns 400 when validation fails")
    void createServiceValidationFails() throws Exception {
        // Blank serviceName and processingDays=0 both violate constraints.
        String body = """
                {"serviceName":"","departmentId":"DEP-1","category":"Certificate","processingDays":0}
                """;

        mockMvc.perform(post("/serviceRequest/createService")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- Service requests

    @Test
    @DisplayName("POST submitRequest returns 201")
    void submitRequestCreated() throws Exception {
        when(serviceRequestService.submitRequest(any()))
                .thenReturn(new MessageResponse("Service request submitted successfully."));

        mockMvc.perform(post("/serviceRequest/submitRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"CIT-1\",\"serviceId\":\"SVC-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Service request submitted successfully."));
    }

    @Test
    @DisplayName("POST submitRequest returns 400 when required fields are missing")
    void submitRequestValidationFails() throws Exception {
        mockMvc.perform(post("/serviceRequest/submitRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citizenId\":\"\",\"serviceId\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET getAllRequests converts the single-letter status code and passes it through")
    void getAllRequestsWithStatusCode() throws Exception {
        when(serviceRequestService.getAllRequests(eq(RequestStatus.Submitted), eq("DEP-1")))
                .thenReturn(List.of(new RequestListItemResponse(
                        "REQ-1", "Birth Certificate", "CIT-1", RequestStatus.Submitted,
                        "DEP-1", LocalDate.now())));

        mockMvc.perform(get("/serviceRequest/getAllRequests")
                        .param("status", "S").param("departmentId", "DEP-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value("REQ-1"))
                .andExpect(jsonPath("$[0].status").value("S"));

        verify(serviceRequestService).getAllRequests(RequestStatus.Submitted, "DEP-1");
    }

    @Test
    @DisplayName("PUT updateRequestStatus returns 200 on a valid transition")
    void updateRequestStatusOk() throws Exception {
        when(serviceRequestService.updateRequestStatus(eq("REQ-1"), any()))
                .thenReturn(new MessageResponse("Request status updated successfully."));

        mockMvc.perform(put("/serviceRequest/updateRequestStatus/REQ-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"U\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT updateRequestStatus maps UnprocessableEntityException to 422")
    void updateRequestStatusInvalidTransition() throws Exception {
        when(serviceRequestService.updateRequestStatus(eq("REQ-1"), any()))
                .thenThrow(new UnprocessableEntityException("Invalid status transition."));

        mockMvc.perform(put("/serviceRequest/updateRequestStatus/REQ-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"A\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Invalid status transition."));
    }

    // ---------------------------------------------------------------- Documents

    @Test
    @DisplayName("POST uploadDocument accepts multipart and returns 201")
    void uploadDocumentCreated() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "id.pdf", "application/pdf", "data".getBytes());
        when(documentService.uploadDocument(eq("REQ-1"), eq("NationalID"), any()))
                .thenReturn(new MessageResponse("Document uploaded successfully."));

        mockMvc.perform(multipart("/serviceRequest/uploadDocument/REQ-1")
                        .file(file).param("documentType", "NationalID"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Document uploaded successfully."));
    }

    @Test
    @DisplayName("PUT verifyDocument returns 200")
    void verifyDocumentOk() throws Exception {
        when(documentService.verifyDocument(eq("DOC-1"), any()))
                .thenReturn(new MessageResponse("Document verified successfully."));

        mockMvc.perform(put("/serviceRequest/verifyDocument/DOC-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificationStatus\":\"V\"}"))
                .andExpect(status().isOk());
    }
}
