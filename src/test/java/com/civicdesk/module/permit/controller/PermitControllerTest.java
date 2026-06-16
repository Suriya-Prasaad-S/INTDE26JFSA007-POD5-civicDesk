package com.civicdesk.module.permit.controller;

import com.civicdesk.module.permit.dto.response.DocumentResponse;
import com.civicdesk.module.permit.dto.response.InspectionResponse;
import com.civicdesk.module.permit.dto.response.PermitDetailResponse;
import com.civicdesk.module.permit.dto.response.PermitSummaryResponse;
import com.civicdesk.module.permit.service.PermitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import com.civicdesk.module.permit.dto.request.PermitDecisionRequest;
import com.civicdesk.module.permit.dto.request.RenewalRequest;
import com.civicdesk.module.permit.dto.request.ScheduleInspectionRequest;
import com.civicdesk.module.permit.dto.request.SubmitPermitRequest;
import com.civicdesk.module.permit.enums.PermitType;
import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
public class PermitControllerTest {

    @Mock
    private PermitService permitService;

    @InjectMocks
    private PermitController permitController;

    // ================================================================
    // createPermit tests
    // ================================================================

   /*
    @Test
    public void createPermit_Success_Returns201() {
        // Arrange
        doNothing().when(permitService).createPermit(any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.createPermit(any(), "cit-00001-0000-0000-000000000001");

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Permit application created successfully",
                response.getBody().get("message"));
    }
    
    */
    @Test
    public void createPermit_Success_Returns201() {
        // Arrange
        SubmitPermitRequest req = new SubmitPermitRequest();
        req.setPermitType(PermitType.BuildingPermit);
        req.setPropertyAddress("12 Anna Nagar, Chennai");
        req.setWard("Ward 5");
        req.setZone("North");

        doNothing().when(permitService).createPermit(any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.createPermit(req, "cit-00001-0000-0000-000000000001");

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Permit application created successfully",
                response.getBody().get("message"));
    }

    /*
    @Test
    public void createPermit_Failure_Returns400() {
        // Arrange
        doThrow(new RuntimeException("error"))
                .when(permitService).createPermit(any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.createPermit(any(), "cit-00001-0000-0000-000000000001");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to create permit application",
                response.getBody().get("message"));
    }
    */
    @Test
    public void createPermit_Failure_Returns400() {
        // Arrange
        SubmitPermitRequest req = new SubmitPermitRequest();
        req.setPermitType(PermitType.BuildingPermit);
        req.setPropertyAddress("12 Anna Nagar, Chennai");
        req.setWard("Ward 5");
        req.setZone("North");

        doThrow(new RuntimeException("error"))
                .when(permitService).createPermit(any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.createPermit(req, "cit-00001-0000-0000-000000000001");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to create permit application",
                response.getBody().get("message"));
    }

    // ================================================================
    // getAllPermits tests
    // ================================================================

    @Test
    public void getAllPermits_Success_Returns200() {
        // Arrange
        PermitSummaryResponse summary = new PermitSummaryResponse();
        summary.setPermitId("perm-0001-test");
        summary.setPermitType("BuildingPermit");
        summary.setStatus("Applied");

        when(permitService.getAllPermits(anyString()))
                .thenReturn(List.of(summary));

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getAllPermits("cit-00001-0000-0000-000000000001");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Permits fetched successfully",
                response.getBody().get("message"));
        assertNotNull(response.getBody().get("permits"));
    }

    @Test
    public void getAllPermits_Empty_ReturnsNoPermitsMessage() {
        // Arrange
        when(permitService.getAllPermits(anyString())).thenReturn(List.of());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getAllPermits("cit-00001-0000-0000-000000000001");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No permits found", response.getBody().get("message"));
    }

    // ================================================================
    // getPermitDetail tests
    // ================================================================

    @Test
    public void getPermitDetail_Success_Returns200() {
        // Arrange
        PermitDetailResponse detail = new PermitDetailResponse();
        detail.setPermitId("perm-0001-test");
        detail.setPermitType("BuildingPermit");
        detail.setStatus("Applied");
        detail.setPropertyAddress("12 Anna Nagar, Chennai");
        detail.setWard("Ward 5");
        detail.setZone("North");
        detail.setFee(15000.00);

        when(permitService.getPermitDetail("perm-0001-test"))
                .thenReturn(detail);

        
        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getPermitDetail("perm-0001-test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Permit details fetched successfully",
                response.getBody().get("message"));
        assertEquals("perm-0001-test", response.getBody().get("permitId"));
    }

    @Test
    public void getPermitDetail_NotFound_Returns404() {
        // Arrange
        doThrow(new RuntimeException("not found"))
                .when(permitService).getPermitDetail("wrong-id");

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getPermitDetail("wrong-id");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Permit not found", response.getBody().get("message"));
    }

    // ================================================================
    // uploadDocument tests
    // ================================================================

    @Test
    public void uploadDocument_Success_Returns201() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "site_plan.pdf",
                "application/pdf", "content".getBytes());

        doNothing().when(permitService)
                .uploadDocuments(any(), any(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.uploadDocument(
                        "perm-0001-test",
                        List.of("SitePlan"),
                        List.of(file));

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Document uploaded successfully",
                response.getBody().get("message"));
    }

    @Test
    public void uploadDocument_Failure_Returns400() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf",
                "application/pdf", "content".getBytes());

        doThrow(new RuntimeException("error"))
                .when(permitService).uploadDocuments(any(), any(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.uploadDocument(
                        "wrong-id",
                        List.of("SitePlan"),
                        List.of(file));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to upload document",
                response.getBody().get("message"));
    }

    // ================================================================
    // getDocuments tests
    // ================================================================

    @Test
    public void getDocuments_Success_Returns200() {
        // Arrange
        DocumentResponse doc = new DocumentResponse();
        doc.setDocumentId("doc-0001-test");
        doc.setDocumentType("SitePlan");
        doc.setVerificationStatus("Pending");
        doc.setUploadedAt(LocalDateTime.now());

        when(permitService.getDocuments("perm-0001-test"))
                .thenReturn(List.of(doc));

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getDocuments("perm-0001-test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Documents fetched successfully",
                response.getBody().get("message"));
    }

    @Test
    public void getDocuments_Empty_ReturnsNoDocumentsMessage() {
        // Arrange
        when(permitService.getDocuments(anyString())).thenReturn(List.of());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getDocuments("perm-0001-test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No documents found for this permit",
                response.getBody().get("message"));
    }

    // ================================================================
    // renewPermit tests
    // ================================================================
/*
    @Test
    public void renewPermit_Success_Returns201() {
        // Arrange
        doNothing().when(permitService).renewPermit(anyString(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.renewPermit("perm-0001-test", any());

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Renewal application created successfully",
                response.getBody().get("message"));
    }
    */
    @Test
    public void renewPermit_Success_Returns201() {
        // Arrange
        RenewalRequest req = new RenewalRequest();
        req.setPropertyAddress("45 Gandhi Street, Chennai");
        req.setWard("Ward 12");
        req.setZone("South");
        req.setValidityPeriod(12);
        req.setFee(5000.00);

        doNothing().when(permitService).renewPermit(anyString(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.renewPermit("perm-0001-test", req);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Renewal application created successfully",
                response.getBody().get("message"));
    }

    /*
    @Test
    public void renewPermit_Failure_Returns400() {
        // Arrange
        doThrow(new RuntimeException("error"))
                .when(permitService).renewPermit(anyString(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.renewPermit("perm-0001-test", any());

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to create renewal application",
                response.getBody().get("message"));
    }
    */
    
    @Test
    public void renewPermit_Failure_Returns400() {
        // Arrange
        RenewalRequest req = new RenewalRequest();
        req.setPropertyAddress("45 Gandhi Street, Chennai");
        req.setWard("Ward 12");
        req.setZone("South");

        doThrow(new RuntimeException("error"))
                .when(permitService).renewPermit(anyString(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.renewPermit("perm-0001-test", req);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to create renewal application",
                response.getBody().get("message"));
    }

    // ================================================================
    // getQueue tests
    // ================================================================

    @Test
    public void getQueue_Success_Returns200() {
        // Arrange
        PermitSummaryResponse summary = new PermitSummaryResponse();
        summary.setPermitId("perm-0001-test");
        summary.setCitizenName("Ravi Kumar");
        summary.setPermitType("BuildingPermit");
        summary.setStatus("Applied");

        when(permitService.getQueue(any(), any()))
                .thenReturn(List.of(summary));

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getQueue(null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Application queue fetched successfully",
                response.getBody().get("message"));
    }

    @Test
    public void getQueue_Empty_ReturnsNoApplicationsMessage() {
        // Arrange
        when(permitService.getQueue(any(), any())).thenReturn(List.of());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getQueue(null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No applications found in queue",
                response.getBody().get("message"));
    }

    // ================================================================
    // scheduleInspection tests
    // ================================================================

   /*
    @Test
    public void scheduleInspection_Success_Returns201() {
        // Arrange
        doNothing().when(permitService).scheduleInspection(anyString(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.scheduleInspection("perm-0001-test", any());

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Inspection scheduled and officer notified successfully",
                response.getBody().get("message"));
    }
    */
    @Test
    public void scheduleInspection_Success_Returns201() {
        // Arrange
        ScheduleInspectionRequest req = new ScheduleInspectionRequest();
        req.setAssignedOfficerId("user-0004-0000-0000-000000000004");
        req.setScheduledDate(LocalDate.of(2025, 2, 10));

        doNothing().when(permitService).scheduleInspection(anyString(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.scheduleInspection("perm-0001-test", req);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Inspection scheduled and officer notified successfully",
                response.getBody().get("message"));
    }

    // ================================================================
    // getInspections tests
    // ================================================================

    @Test
    public void getInspections_Success_Returns200() {
        // Arrange
        InspectionResponse inspection = new InspectionResponse();
        inspection.setInspectionId("insp-0001-test");
        inspection.setStatus("Scheduled");

        when(permitService.getInspections("perm-0001-test"))
                .thenReturn(List.of(inspection));

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getInspections("perm-0001-test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Inspection results fetched successfully",
                response.getBody().get("message"));
    }

    // ================================================================
    // makeDecision tests
    // ================================================================
/*
    @Test
    public void makeDecision_Approved_Returns200() {
        // Arrange
        doNothing().when(permitService).makeDecision(anyString(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.makeDecision("perm-0001-test", any());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Permit decision updated successfully",
                response.getBody().get("message"));
    }

*/
    
    @Test
    public void makeDecision_Approved_Returns200() {
        // Arrange
        PermitDecisionRequest req = new PermitDecisionRequest();
        req.setDecision("Approved");
        req.setRejectionReason(null);

        doNothing().when(permitService).makeDecision(anyString(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.makeDecision("perm-0001-test", req);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Permit decision updated successfully",
                response.getBody().get("message"));
    }
    // ================================================================
    // getMyAssignments tests
    // ================================================================

    @Test
    public void getMyAssignments_Success_Returns200() {
        // Arrange
        InspectionResponse inspection = new InspectionResponse();
        inspection.setInspectionId("insp-0001-test");
        inspection.setStatus("Scheduled");

        when(permitService.getMyAssignments(anyString()))
                .thenReturn(List.of(inspection));

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.getMyAssignments(
                        "user-0004-0000-0000-000000000004");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Assigned inspections fetched successfully",
                response.getBody().get("message"));
    }

    // ================================================================
    // submitOutcome tests
    // ================================================================

    @Test
    public void submitOutcome_Success_Returns200() {
        // Arrange
        doNothing().when(permitService)
                .submitInspectionOutcome(anyString(), anyString(),
                        any(), any(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.submitOutcome(
                        "insp-0001-test", "Pass",
                        "Premises clean.", "13.0827,80.2707", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Inspection outcome submitted successfully",
                response.getBody().get("message"));
    }

    @Test
    public void submitOutcome_Failure_Returns400() {
        // Arrange
        doThrow(new RuntimeException("error"))
                .when(permitService).submitInspectionOutcome(
                        anyString(), anyString(), any(), any(), any());

        // Act
        ResponseEntity<Map<String, Object>> response =
                permitController.submitOutcome(
                        "wrong-id", "Pass", null, null, null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to submit inspection outcome",
                response.getBody().get("message"));
    }
}