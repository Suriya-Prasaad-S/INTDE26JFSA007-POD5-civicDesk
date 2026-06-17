package com.civicdesk.module.citizen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.civicdesk.common.exception.citizen.BusinessRuleException;
import com.civicdesk.common.exception.citizen.ForbiddenActionException;
import com.civicdesk.common.exception.citizen.InvalidRequestException;
import com.civicdesk.common.exception.citizen.ResourceNotFoundException;
import com.civicdesk.module.citizen.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.citizen.dto.response.DocumentDetailResponse;
import com.civicdesk.module.citizen.dto.response.DocumentSummaryResponse;
import com.civicdesk.module.citizen.entity.CitizenDocument;
import com.civicdesk.module.citizen.entity.enums.DocumentStatus;
import com.civicdesk.module.citizen.entity.enums.DocumentType;
import com.civicdesk.module.citizen.repository.CitizenDocumentRepository;
import com.civicdesk.module.citizen.repository.CitizenProfileRepository;

/** Unit tests for {@link DocumentService}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    private static final String CITIZEN = "cit-1";
    private static final String FILE_PATH = "http://localhost/civicDesk/citizenProfile/files/abc.pdf";

    @Mock CitizenDocumentRepository documentRepository;
    @Mock CitizenProfileRepository citizenRepository;

    DocumentService service;

    @BeforeEach
    void setup() {
        service = new DocumentService(documentRepository, citizenRepository);
        authenticateAs(CITIZEN, "CIT");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    // ---------------------------------------------------------------------------------------------
    // uploadDocument
    // ---------------------------------------------------------------------------------------------

    @Test
    void uploadDocument_valid_savesAsValidAndReturnsId() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.countByCitizenId(CITIZEN)).thenReturn(0L);
        when(documentRepository.save(any(CitizenDocument.class))).thenAnswer(i -> {
            CitizenDocument d = i.getArgument(0);
            d.setDocumentId("50000001");
            return d;
        });

        String id = service.uploadDocument(CITIZEN, "NationalID", "id.pdf",
                "application/pdf", 1500, FILE_PATH);

        assertThat(id).isEqualTo("50000001");
        ArgumentCaptor<CitizenDocument> captor = ArgumentCaptor.forClass(CitizenDocument.class);
        verify(documentRepository).save(captor.capture());
        CitizenDocument saved = captor.getValue();
        assertThat(saved.getCitizenId()).isEqualTo(CITIZEN);
        assertThat(saved.getDocumentType()).isEqualTo(DocumentType.NationalID);
        assertThat(saved.getFileName()).isEqualTo("id.pdf");
        assertThat(saved.getFilePath()).isEqualTo(FILE_PATH);
        assertThat(saved.getFileType()).isEqualTo("pdf");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.Valid);
        // 1500 bytes -> ceil(1500/1024) = 2 KB
        assertThat(saved.getFileSizeKb()).isEqualTo(2);
    }

    @Test
    void uploadDocument_uppercaseMime_isAcceptedCaseInsensitively() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.countByCitizenId(CITIZEN)).thenReturn(0L);
        when(documentRepository.save(any(CitizenDocument.class))).thenAnswer(i -> i.getArgument(0));

        service.uploadDocument(CITIZEN, "ResidenceProof", "photo.JPG", "IMAGE/JPEG", 1024, FILE_PATH);

        verify(documentRepository).save(any(CitizenDocument.class));
    }

    @Test
    void uploadDocument_notSelf_throwsForbidden() {
        assertThatThrownBy(() -> service.uploadDocument("someone-else", "NationalID", "id.pdf",
                "application/pdf", 1000, FILE_PATH))
                .isInstanceOf(ForbiddenActionException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_citizenDoesNotExist_throwsResourceNotFound() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(false);
        assertThatThrownBy(() -> service.uploadDocument(CITIZEN, "NationalID", "id.pdf",
                "application/pdf", 1000, FILE_PATH))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void uploadDocument_invalidDocumentType_throwsInvalidRequest() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        assertThatThrownBy(() -> service.uploadDocument(CITIZEN, "Passport", "id.pdf",
                "application/pdf", 1000, FILE_PATH))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void uploadDocument_limitReached_throwsBusinessRule() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.countByCitizenId(CITIZEN)).thenReturn(5L);
        assertThatThrownBy(() -> service.uploadDocument(CITIZEN, "NationalID", "id.pdf",
                "application/pdf", 1000, FILE_PATH))
                .isInstanceOf(BusinessRuleException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_emptyFile_throwsInvalidRequest() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.countByCitizenId(CITIZEN)).thenReturn(0L);
        assertThatThrownBy(() -> service.uploadDocument(CITIZEN, "NationalID", "id.pdf",
                "application/pdf", 0, FILE_PATH))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void uploadDocument_tooLarge_throwsInvalidRequest() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.countByCitizenId(CITIZEN)).thenReturn(0L);
        long tooBig = 2L * 1024 * 1024 + 1;
        assertThatThrownBy(() -> service.uploadDocument(CITIZEN, "NationalID", "id.pdf",
                "application/pdf", tooBig, FILE_PATH))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void uploadDocument_unsupportedExtension_throwsInvalidRequest() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.countByCitizenId(CITIZEN)).thenReturn(0L);
        assertThatThrownBy(() -> service.uploadDocument(CITIZEN, "NationalID", "virus.exe",
                "application/pdf", 1000, FILE_PATH))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void uploadDocument_unsupportedMime_throwsInvalidRequest() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.countByCitizenId(CITIZEN)).thenReturn(0L);
        assertThatThrownBy(() -> service.uploadDocument(CITIZEN, "NationalID", "id.pdf",
                "text/plain", 1000, FILE_PATH))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // getAllDocuments
    // ---------------------------------------------------------------------------------------------

    @Test
    void getAllDocuments_returnsSummaries() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.findByCitizenId(CITIZEN))
                .thenReturn(List.of(document("d1", DocumentStatus.Valid), document("d2", DocumentStatus.Revoked)));

        List<DocumentSummaryResponse> result = service.getAllDocuments(CITIZEN);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DocumentSummaryResponse::documentId).contains("d1", "d2");
    }

    @Test
    void getAllDocuments_notSelf_throwsForbidden() {
        assertThatThrownBy(() -> service.getAllDocuments("someone-else"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void getAllDocuments_citizenDoesNotExist_throwsResourceNotFound() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(false);
        assertThatThrownBy(() -> service.getAllDocuments(CITIZEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllDocuments_expiredValidDocument_reportsEffectiveExpired() {
        CitizenDocument d = document("d1", DocumentStatus.Valid);
        d.setExpiryDate(LocalDate.now().minusDays(1)); // still stored Valid but past expiry
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.findByCitizenId(CITIZEN)).thenReturn(List.of(d));

        List<DocumentSummaryResponse> result = service.getAllDocuments(CITIZEN);

        assertThat(result.get(0).status()).isEqualTo("E");
    }

    // ---------------------------------------------------------------------------------------------
    // getDocumentById
    // ---------------------------------------------------------------------------------------------

    @Test
    void getDocumentById_returnsDetail() {
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN))
                .thenReturn(Optional.of(document("d1", DocumentStatus.Valid)));

        DocumentDetailResponse res = service.getDocumentById(CITIZEN, "d1");

        assertThat(res.documentId()).isEqualTo("d1");
        assertThat(res.citizenId()).isEqualTo(CITIZEN);
        assertThat(res.status()).isEqualTo("V");
    }

    @Test
    void getDocumentById_notSelf_throwsForbidden() {
        assertThatThrownBy(() -> service.getDocumentById("someone-else", "d1"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void getDocumentById_notFound_throwsResourceNotFound() {
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDocumentById(CITIZEN, "d1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // verifyDocument
    // ---------------------------------------------------------------------------------------------

    @Test
    void verifyDocument_validToRevoked_savesAndStampsVerifier() {
        authenticateAs("officer-1", "DS");
        CitizenDocument d = document("d1", DocumentStatus.Valid);
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN)).thenReturn(Optional.of(d));
        when(documentRepository.save(any(CitizenDocument.class))).thenAnswer(i -> i.getArgument(0));

        service.verifyDocument(CITIZEN, "d1", new VerifyDocumentRequest("R"));

        assertThat(d.getStatus()).isEqualTo(DocumentStatus.Revoked);
        assertThat(d.getVerifiedBy()).isEqualTo("officer-1");
        assertThat(d.getVerifiedAt()).isNotNull();
        verify(documentRepository).save(d);
    }

    @Test
    void verifyDocument_validToValid_confirmIsAllowed() {
        authenticateAs("officer-1", "DS");
        CitizenDocument d = document("d1", DocumentStatus.Valid);
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN)).thenReturn(Optional.of(d));
        when(documentRepository.save(any(CitizenDocument.class))).thenAnswer(i -> i.getArgument(0));

        service.verifyDocument(CITIZEN, "d1", new VerifyDocumentRequest("V"));

        assertThat(d.getStatus()).isEqualTo(DocumentStatus.Valid);
        assertThat(d.getVerifiedBy()).isEqualTo("officer-1");
    }

    @Test
    void verifyDocument_expiredToRevoked_allowedViaEffectiveStatus() {
        authenticateAs("officer-1", "DS");
        CitizenDocument d = document("d1", DocumentStatus.Valid);
        d.setExpiryDate(LocalDate.now().minusDays(1)); // effectively Expired
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN)).thenReturn(Optional.of(d));
        when(documentRepository.save(any(CitizenDocument.class))).thenAnswer(i -> i.getArgument(0));

        service.verifyDocument(CITIZEN, "d1", new VerifyDocumentRequest("R"));

        assertThat(d.getStatus()).isEqualTo(DocumentStatus.Revoked);
    }

    @Test
    void verifyDocument_notFound_throwsResourceNotFound() {
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifyDocument(CITIZEN, "d1", new VerifyDocumentRequest("R")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifyDocument_invalidStatusCode_throwsInvalidRequest() {
        CitizenDocument d = document("d1", DocumentStatus.Valid);
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.verifyDocument(CITIZEN, "d1", new VerifyDocumentRequest("Z")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyDocument_expiredIsNeverAManualTarget_throwsBusinessRule() {
        CitizenDocument d = document("d1", DocumentStatus.Valid);
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.verifyDocument(CITIZEN, "d1", new VerifyDocumentRequest("E")))
                .isInstanceOf(BusinessRuleException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void verifyDocument_revokedIsTerminal_throwsBusinessRule() {
        CitizenDocument d = document("d1", DocumentStatus.Revoked);
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.verifyDocument(CITIZEN, "d1", new VerifyDocumentRequest("V")))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // fixtures
    // ---------------------------------------------------------------------------------------------

    private CitizenDocument document(String id, DocumentStatus status) {
        CitizenDocument d = new CitizenDocument();
        d.setDocumentId(id);
        d.setCitizenId(CITIZEN);
        d.setDocumentType(DocumentType.NationalID);
        d.setFileName("id.pdf");
        d.setFilePath(FILE_PATH);
        d.setFileType("pdf");
        d.setFileSizeKb(2);
        d.setStatus(status);
        return d;
    }
}
