package com.civicdesk.module.permit.service;
import java.util.List;
import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.module.permit.dto.request.PermitDecisionRequest;
import com.civicdesk.module.permit.dto.request.RenewalRequest;
import com.civicdesk.module.permit.dto.request.RequestDocumentsRequest;
import com.civicdesk.module.permit.dto.request.ScheduleInspectionRequest;
import com.civicdesk.module.permit.dto.request.SubmitPermitRequest;
import com.civicdesk.module.permit.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.permit.dto.response.DocumentResponse;
import com.civicdesk.module.permit.dto.response.InspectionResponse;
import com.civicdesk.module.permit.dto.response.PermitDetailResponse;
import com.civicdesk.module.permit.dto.response.PermitSummaryResponse;
import com.civicdesk.module.permit.entity.CitizenProfile;
import com.civicdesk.module.permit.entity.Inspection;
import com.civicdesk.module.permit.entity.PermitApplication;
import com.civicdesk.module.permit.entity.PermitDocument;
import com.civicdesk.module.permit.entity.User;
import com.civicdesk.module.permit.enums.DocumentType;
import com.civicdesk.module.permit.enums.InspectionStatus;
import com.civicdesk.module.permit.enums.PermitStatus;
import com.civicdesk.module.permit.enums.PermitType;
import com.civicdesk.module.permit.repository.CitizenProfileRepository;
import com.civicdesk.module.permit.repository.InspectionRepository;
import com.civicdesk.module.permit.repository.PermitApplicationRepository;
import com.civicdesk.module.permit.repository.PermitDocumentRepository;
import com.civicdesk.module.permit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PermitServiceTest {

    @Mock
    private PermitApplicationRepository permitRepo;

    @Mock
    private PermitDocumentRepository documentRepo;

    @Mock
    private InspectionRepository inspectionRepo;

    @Mock
    private CitizenProfileRepository citizenRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private FileStorageService fileStorage;

    @InjectMocks
    private PermitService permitService;

    private CitizenProfile mockCitizen;
    private PermitApplication mockPermit;
    private PermitDocument mockDocument;
    private Inspection mockInspection;
    private User mockUser;

    @BeforeEach
    public void setUp() {
        mockCitizen = new CitizenProfile();
        mockCitizen.setCitizenId("cit-00001-0000-0000-000000000001");
        mockCitizen.setUserId("user-0007-0000-0000-000000000007");
        mockCitizen.setGender("MALE");
        mockCitizen.setProfileStatus("Verified");

        mockPermit = new PermitApplication();
        mockPermit.setPermitId("perm-0001-test");
        mockPermit.setCitizenId("cit-00001-0000-0000-000000000001");
        mockPermit.setPermitType(PermitType.BuildingPermit);
        mockPermit.setApplicationDate(LocalDate.now());
        mockPermit.setPropertyAddress("12 Anna Nagar, Chennai");
        mockPermit.setWard("Ward 5");
        mockPermit.setZone("North");
        mockPermit.setValidityPeriod(24);
        mockPermit.setFee(15000.00);
        mockPermit.setStatus(PermitStatus.Applied);
        mockPermit.setDeleted(false);

        mockDocument = new PermitDocument();
        mockDocument.setDocumentId("doc-0001-test");
        mockDocument.setPermitId("perm-0001-test");
        mockDocument.setDocumentType(DocumentType.SitePlan);
        mockDocument.setFilePath("/uploads/permits/perm0001/site_plan.pdf");
        mockDocument.setVerificationStatus("Pending");
        mockDocument.setUploadedAt(LocalDateTime.now());
        mockDocument.setDeleted(false);

        mockInspection = new Inspection();
        mockInspection.setInspectionId("insp-0001-test");
        mockInspection.setPermitId("perm-0001-test");
        mockInspection.setAssignedOfficerId("user-0004-0000-0000-000000000004");
        mockInspection.setScheduledDate(LocalDate.of(2025, 2, 10));
        mockInspection.setStatus(InspectionStatus.Scheduled);

        mockUser = new User();
        mockUser.setUserId("user-0004-0000-0000-000000000004");
        mockUser.setName("Priya Officer");
        mockUser.setRole("FIELD_OFFICER");
    }

    // ================================================================
    // createPermit tests
    // ================================================================

    @Test
    public void createPermit_Success() {
        // Arrange
        SubmitPermitRequest req = new SubmitPermitRequest();
        req.setCitizenId("cit-00001-0000-0000-000000000001");
        req.setPermitType(PermitType.BuildingPermit);
        req.setPropertyAddress("12 Anna Nagar, Chennai");
        req.setWard("Ward 5");
        req.setZone("North");
        req.setValidityPeriod(24);
        req.setFee(15000.00);

        when(citizenRepo.findByCitizenId("cit-00001-0000-0000-000000000001"))
                .thenReturn(Optional.of(mockCitizen));
        when(permitRepo.save(any(PermitApplication.class)))
                .thenReturn(mockPermit);

        // Act
        permitService.createPermit(req);

        // Assert
        verify(permitRepo, times(1)).save(any(PermitApplication.class));
    }

    @Test
    public void createPermit_CitizenNotFound_ThrowsException() {
        // Arrange
        SubmitPermitRequest req = new SubmitPermitRequest();
        req.setCitizenId("wrong-citizen-id");
        req.setPermitType(PermitType.BuildingPermit);
        req.setPropertyAddress("12 Anna Nagar, Chennai");
        req.setWard("Ward 5");
        req.setZone("North");

        when(citizenRepo.findByCitizenId("wrong-citizen-id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> permitService.createPermit(req));
    }

    // ================================================================
    // getAllPermits tests
    // ================================================================

    @Test
    public void getAllPermits_Success_ReturnsList() {
        // Arrange
        when(permitRepo.findByCitizenIdAndIsDeletedFalse(
                "cit-00001-0000-0000-000000000001"))
                .thenReturn(List.of(mockPermit));

        // Act
        List<PermitSummaryResponse> result = permitService.getAllPermits(
                "cit-00001-0000-0000-000000000001");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("perm-0001-test", result.get(0).getPermitId());
        assertEquals("BuildingPermit", result.get(0).getPermitType());
    }

    @Test
    public void getAllPermits_NoPermits_ReturnsEmptyList() {
        // Arrange
        when(permitRepo.findByCitizenIdAndIsDeletedFalse(anyString()))
                .thenReturn(List.of());

        // Act
        List<PermitSummaryResponse> result = permitService.getAllPermits(
                "cit-00001-0000-0000-000000000001");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ================================================================
    // getPermitDetail tests
    // ================================================================

    @Test
    public void getPermitDetail_Success() {
        // Arrange
        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));

        // Act
        PermitDetailResponse result = permitService.getPermitDetail("perm-0001-test");

        // Assert
        assertNotNull(result);
        assertEquals("perm-0001-test",        result.getPermitId());
        assertEquals("BuildingPermit",         result.getPermitType());
        assertEquals("Applied",                result.getStatus());
        assertEquals("12 Anna Nagar, Chennai", result.getPropertyAddress());
    }

    @Test
    public void getPermitDetail_PermitNotFound_ThrowsException() {
        // Arrange
        when(permitRepo.findByPermitIdAndIsDeletedFalse("wrong-id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> permitService.getPermitDetail("wrong-id"));
    }

    // ================================================================
    // uploadDocument tests
    // ================================================================

    @Test
    public void uploadDocument_Success() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "site_plan.pdf",
                "application/pdf", "dummy content".getBytes());

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(fileStorage.store(any(), anyString()))
                .thenReturn("/uploads/permits/perm0001/site_plan.pdf");
        when(documentRepo.save(any(PermitDocument.class)))
                .thenReturn(mockDocument);

        // Act
        permitService.uploadDocuments(
                "perm-0001-test",
                List.of("SitePlan"),
                List.of(file));

        // Assert
        verify(documentRepo, times(1)).save(any(PermitDocument.class));
    }

    @Test
    public void uploadDocument_InvalidDocumentType_ThrowsException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf",
                "application/pdf", "content".getBytes());

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));

        // Act & Assert
        assertThrows(BadRequestException.class,
                () -> permitService.uploadDocuments(
                        "perm-0001-test",
                        List.of("InvalidType"),
                        List.of(file)));
    }

    @Test
    public void uploadDocument_PermitNotFound_ThrowsException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf",
                "application/pdf", "content".getBytes());

        when(permitRepo.findByPermitIdAndIsDeletedFalse("wrong-id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> permitService.uploadDocuments(
                        "wrong-id",
                        List.of("SitePlan"),
                        List.of(file)));
    }

    // ================================================================
    // getDocuments tests
    // ================================================================

    @Test
    public void getDocuments_Success_ReturnsList() {
        // Arrange
        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(documentRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(List.of(mockDocument));

        // Act
        List<DocumentResponse> result = permitService.getDocuments("perm-0001-test");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("doc-0001-test", result.get(0).getDocumentId());
        assertEquals("SitePlan",      result.get(0).getDocumentType());
        assertEquals("Pending",        result.get(0).getVerificationStatus());
    }

    // ================================================================
    // renewPermit tests
    // ================================================================

    @Test
    public void renewPermit_Success_TradeLicense() {
        // Arrange
        mockPermit.setPermitType(PermitType.TradeLicense);

        RenewalRequest req = new RenewalRequest();
        req.setPropertyAddress("45 Gandhi Street, Chennai");
        req.setWard("Ward 12");
        req.setZone("South");
        req.setValidityPeriod(12);
        req.setFee(5000.00);

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(permitRepo.save(any(PermitApplication.class)))
                .thenReturn(mockPermit);

        // Act
        permitService.renewPermit("perm-0001-test", req);

        // Assert
        verify(permitRepo, times(1)).save(any(PermitApplication.class));
    }

    @Test
    public void renewPermit_BuildingPermit_ThrowsException() {
        // Arrange
        mockPermit.setPermitType(PermitType.BuildingPermit);

        RenewalRequest req = new RenewalRequest();
        req.setPropertyAddress("12 Anna Nagar, Chennai");
        req.setWard("Ward 5");
        req.setZone("North");

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));

        // Act & Assert
        assertThrows(BadRequestException.class,
                () -> permitService.renewPermit("perm-0001-test", req));
    }

    // ================================================================
    // requestDocuments tests
    // ================================================================

    @Test
    public void requestDocuments_Success() {
        // Arrange
        RequestDocumentsRequest req = new RequestDocumentsRequest();
        req.setRemarks("Site plan is missing structural engineer stamp.");

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(permitRepo.save(any(PermitApplication.class)))
                .thenReturn(mockPermit);

        // Act
        permitService.requestDocuments("perm-0001-test", req);

        // Assert
        verify(permitRepo, times(1)).save(any(PermitApplication.class));
        assertEquals(PermitStatus.PendingDocuments, mockPermit.getStatus());
    }

    // ================================================================
    // verifyDocument tests
    // ================================================================

    @Test
    public void verifyDocument_Success_Verified() {
        // Arrange
        VerifyDocumentRequest req = new VerifyDocumentRequest();
        req.setVerificationStatus("Verified");
        req.setVerificationRemarks(null);

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(documentRepo.findByDocumentIdAndIsDeletedFalse("doc-0001-test"))
                .thenReturn(Optional.of(mockDocument));
        when(documentRepo.save(any(PermitDocument.class)))
                .thenReturn(mockDocument);

        // Act
        permitService.verifyDocument("perm-0001-test", "doc-0001-test", req);

        // Assert
        verify(documentRepo, times(1)).save(any(PermitDocument.class));
        assertEquals("Verified", mockDocument.getVerificationStatus());
    }

    @Test
    public void verifyDocument_Rejected_WithRemarks() {
        // Arrange
        VerifyDocumentRequest req = new VerifyDocumentRequest();
        req.setVerificationStatus("Rejected");
        req.setVerificationRemarks("Site plan is missing structural engineer stamp.");

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(documentRepo.findByDocumentIdAndIsDeletedFalse("doc-0001-test"))
                .thenReturn(Optional.of(mockDocument));
        when(documentRepo.save(any(PermitDocument.class)))
                .thenReturn(mockDocument);

        // Act
        permitService.verifyDocument("perm-0001-test", "doc-0001-test", req);

        // Assert
        assertEquals("Rejected", mockDocument.getVerificationStatus());
        assertEquals("Site plan is missing structural engineer stamp.",
                mockDocument.getVerificationRemarks());
    }

    @Test
    public void verifyDocument_DocumentNotFound_ThrowsException() {
        // Arrange
        VerifyDocumentRequest req = new VerifyDocumentRequest();
        req.setVerificationStatus("Verified");

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(documentRepo.findByDocumentIdAndIsDeletedFalse("wrong-doc-id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> permitService.verifyDocument(
                        "perm-0001-test", "wrong-doc-id", req));
    }

    // ================================================================
    // scheduleInspection tests
    // ================================================================

    @Test
    public void scheduleInspection_Success() {
        // Arrange
        ScheduleInspectionRequest req = new ScheduleInspectionRequest();
        req.setAssignedOfficerId("user-0004-0000-0000-000000000004");
        req.setScheduledDate(LocalDate.of(2025, 2, 10));

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(userRepo.findByUserId("user-0004-0000-0000-000000000004"))
                .thenReturn(Optional.of(mockUser));
        when(inspectionRepo.save(any(Inspection.class)))
                .thenReturn(mockInspection);
        when(permitRepo.save(any(PermitApplication.class)))
                .thenReturn(mockPermit);

        // Act
        permitService.scheduleInspection("perm-0001-test", req);

        // Assert
        verify(inspectionRepo, times(1)).save(any(Inspection.class));
        assertEquals(PermitStatus.InspectionScheduled, mockPermit.getStatus());
    }

    @Test
    public void scheduleInspection_OfficerNotFound_ThrowsException() {
        // Arrange
        ScheduleInspectionRequest req = new ScheduleInspectionRequest();
        req.setAssignedOfficerId("wrong-officer-id");
        req.setScheduledDate(LocalDate.of(2025, 2, 10));

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(userRepo.findByUserId("wrong-officer-id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> permitService.scheduleInspection("perm-0001-test", req));
    }

    // ================================================================
    // getInspections tests
    // ================================================================

    @Test
    public void getInspections_Success_ReturnsList() {
        // Arrange
        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(inspectionRepo.findByPermitId("perm-0001-test"))
                .thenReturn(List.of(mockInspection));
        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));

        // Act
        List<InspectionResponse> result = permitService.getInspections("perm-0001-test");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("insp-0001-test", result.get(0).getInspectionId());
        assertEquals("Scheduled",       result.get(0).getStatus());
    }

    // ================================================================
    // makeDecision tests
    // ================================================================

    @Test
    public void makeDecision_Approved_Success() {
        // Arrange
        mockPermit.setValidityPeriod(24);

        PermitDecisionRequest req = new PermitDecisionRequest();
        req.setDecision("Approved");
        req.setRejectionReason(null);

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(permitRepo.save(any(PermitApplication.class)))
                .thenReturn(mockPermit);

        // Act
        permitService.makeDecision("perm-0001-test", req);

        // Assert
        assertEquals(PermitStatus.Approved,  mockPermit.getStatus());
        assertNotNull(mockPermit.getValidFrom());
        assertNotNull(mockPermit.getValidUntil());
    }

    @Test
    public void makeDecision_Rejected_Success() {
        // Arrange
        PermitDecisionRequest req = new PermitDecisionRequest();
        req.setDecision("Rejected");
        req.setRejectionReason("Property does not meet fire safety norms.");

        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));
        when(permitRepo.save(any(PermitApplication.class)))
                .thenReturn(mockPermit);

        // Act
        permitService.makeDecision("perm-0001-test", req);

        // Assert
        assertEquals(PermitStatus.Rejected, mockPermit.getStatus());
        assertEquals("Property does not meet fire safety norms.",
                mockPermit.getRejectionReason());
    }

    @Test
    public void makeDecision_InvalidDecision_ThrowsException() {
        // Arrange
        PermitDecisionRequest req = new PermitDecisionRequest();
        req.setDecision("InvalidDecision");

        // No stub needed — exception is thrown before repo is called

        // Act & Assert
        assertThrows(BadRequestException.class,
                () -> permitService.makeDecision("perm-0001-test", req));
    }

    // ================================================================
    // getMyAssignments tests
    // ================================================================

    @Test
    public void getMyAssignments_Success_ReturnsList() {
        // Arrange
        when(inspectionRepo.findByAssignedOfficerId(
                "user-0004-0000-0000-000000000004"))
                .thenReturn(List.of(mockInspection));
        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));

        // Act
        List<InspectionResponse> result = permitService.getMyAssignments(
                "user-0004-0000-0000-000000000004");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("insp-0001-test", result.get(0).getInspectionId());
    }

    // ================================================================
    // getInspectionDetail tests
    // ================================================================

    @Test
    public void getInspectionDetail_Success() {
        // Arrange
        when(inspectionRepo.findByInspectionId("insp-0001-test"))
                .thenReturn(Optional.of(mockInspection));
        when(permitRepo.findByPermitIdAndIsDeletedFalse("perm-0001-test"))
                .thenReturn(Optional.of(mockPermit));

        // Act
        InspectionResponse result = permitService.getInspectionDetail("insp-0001-test");

        // Assert
        assertNotNull(result);
        assertEquals("insp-0001-test", result.getInspectionId());
        assertEquals("perm-0001-test", result.getPermitId());
    }

    @Test
    public void getInspectionDetail_NotFound_ThrowsException() {
        // Arrange
        when(inspectionRepo.findByInspectionId("wrong-id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> permitService.getInspectionDetail("wrong-id"));
    }

    // ================================================================
    // submitInspectionOutcome tests
    // ================================================================

    @Test
    public void submitInspectionOutcome_Success() {
        // Arrange
        when(inspectionRepo.findByInspectionId("insp-0001-test"))
                .thenReturn(Optional.of(mockInspection));
        when(inspectionRepo.save(any(Inspection.class)))
                .thenReturn(mockInspection);

        // Act
        permitService.submitInspectionOutcome(
                "insp-0001-test", "Pass",
                "Premises clean.", "13.0827,80.2707", null);

        // Assert
        verify(inspectionRepo, times(1)).save(any(Inspection.class));
        assertEquals(InspectionStatus.Completed, mockInspection.getStatus());
        assertEquals("Pass",             mockInspection.getOutcome());
        assertEquals("Premises clean.",  mockInspection.getRemarks());
    }

    @Test
    public void submitInspectionOutcome_InspectionNotFound_ThrowsException() {
        // Arrange
        when(inspectionRepo.findByInspectionId("wrong-id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> permitService.submitInspectionOutcome(
                        "wrong-id", "Pass", null, null, null));
    }
}