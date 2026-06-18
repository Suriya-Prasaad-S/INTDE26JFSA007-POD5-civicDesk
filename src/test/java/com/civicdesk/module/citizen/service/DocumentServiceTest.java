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

import com.civicdesk.common.exception.citizen.ForbiddenActionException;
import com.civicdesk.common.exception.citizen.InvalidRequestException;
import com.civicdesk.common.exception.citizen.ResourceNotFoundException;
import com.civicdesk.module.citizen.dto.response.DocumentDetailResponse;
import com.civicdesk.module.citizen.dto.response.DocumentSummaryResponse;
import com.civicdesk.module.citizen.entity.CitizenDocument;
import com.civicdesk.module.citizen.entity.enums.DocumentStatus;
import com.civicdesk.module.citizen.entity.enums.DocumentType;
import com.civicdesk.module.citizen.repository.CitizenDocumentRepository;
import com.civicdesk.module.citizen.repository.CitizenProfileRepository;
import com.civicdesk.module.citizen.support.FileStorageService;

/** Unit tests for {@link DocumentService} — the citizen document wallet. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    private static final String CITIZEN = "cit-1";
    private static final String FILE_PATH = "abc.pdf";

    @Mock CitizenDocumentRepository documentRepository;
    @Mock CitizenProfileRepository citizenRepository;
    @Mock FileStorageService fileStorage;

    DocumentService service;

    @BeforeEach
    void setup() {
        service = new DocumentService(documentRepository, citizenRepository, fileStorage);
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
    // addDocument (in-process push by the service-request module)
    // ---------------------------------------------------------------------------------------------

    @Test
    void addDocument_storesFileAndSavesValidRecord() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(fileStorage.exists(any())).thenReturn(true);
        when(documentRepository.save(any(CitizenDocument.class))).thenAnswer(i -> {
            CitizenDocument d = i.getArgument(0);
            d.setDocumentId("50000001");
            return d;
        });

        String id = service.addDocument(CITIZEN, "BirthCertificate", "birth.pdf",
                "pdf-bytes".getBytes(), LocalDate.of(2024, 1, 1), null);

        assertThat(id).isEqualTo("50000001");
        verify(fileStorage).store(any(), any());
        ArgumentCaptor<CitizenDocument> captor = ArgumentCaptor.forClass(CitizenDocument.class);
        verify(documentRepository).save(captor.capture());
        CitizenDocument saved = captor.getValue();
        assertThat(saved.getCitizenId()).isEqualTo(CITIZEN);
        assertThat(saved.getDocumentType()).isEqualTo(DocumentType.BirthCertificate);
        assertThat(saved.getFileName()).isEqualTo("birth.pdf");
        assertThat(saved.getFileType()).isEqualTo("pdf");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.Valid);
        assertThat(saved.getIssuedDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    void addDocument_citizenDoesNotExist_throwsResourceNotFound() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(false);
        assertThatThrownBy(() -> service.addDocument(CITIZEN, "NationalID", "id.pdf",
                "x".getBytes(), null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void addDocument_invalidDocumentType_throwsInvalidRequest() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        assertThatThrownBy(() -> service.addDocument(CITIZEN, "Passport", "id.pdf",
                "x".getBytes(), null, null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void addDocument_fileNotStored_throwsAndDoesNotSaveRecord() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(fileStorage.exists(any())).thenReturn(false); // store landed nowhere

        assertThatThrownBy(() -> service.addDocument(CITIZEN, "NationalID", "id.pdf",
                "x".getBytes(), null, null))
                .isInstanceOf(InvalidRequestException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void addDocument_emptyContent_throwsInvalidRequest() {
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        assertThatThrownBy(() -> service.addDocument(CITIZEN, "NationalID", "id.pdf",
                new byte[0], null, null))
                .isInstanceOf(InvalidRequestException.class);
        verify(documentRepository, never()).save(any());
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
        d.setExpiryDate(LocalDate.now().minusDays(1));
        when(citizenRepository.existsById(CITIZEN)).thenReturn(true);
        when(documentRepository.findByCitizenId(CITIZEN)).thenReturn(List.of(d));

        List<DocumentSummaryResponse> result = service.getAllDocuments(CITIZEN);

        assertThat(result.get(0).status()).isEqualTo("E");
    }

    // ---------------------------------------------------------------------------------------------
    // getDocumentById / resolveDownloadFileName
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

    @Test
    void resolveDownloadFileName_returnsStoredFileName() {
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN))
                .thenReturn(Optional.of(document("d1", DocumentStatus.Valid)));
        assertThat(service.resolveDownloadFileName(CITIZEN, "d1")).isEqualTo(FILE_PATH);
    }

    @Test
    void resolveDownloadFileName_notSelf_throwsForbidden() {
        assertThatThrownBy(() -> service.resolveDownloadFileName("someone-else", "d1"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void resolveDownloadFileName_notFound_throwsResourceNotFound() {
        when(documentRepository.findByDocumentIdAndCitizenId("d1", CITIZEN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolveDownloadFileName(CITIZEN, "d1"))
                .isInstanceOf(ResourceNotFoundException.class);
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
