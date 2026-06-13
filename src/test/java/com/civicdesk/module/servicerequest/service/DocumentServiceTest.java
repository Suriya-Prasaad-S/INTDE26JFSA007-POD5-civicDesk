package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.exception.UnprocessableEntityException;
import com.civicdesk.module.serviceRequest.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.serviceRequest.dto.response.DocumentItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.MessageResponse;
import com.civicdesk.module.serviceRequest.entity.RequestDocument;
import com.civicdesk.module.serviceRequest.entity.ServiceRequest;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.enums.VerificationStatus;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.repository.RequestDocumentRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentService}: upload guards, listing, and the officer
 * verification flow (including the rejection side effect on the parent request).
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private RequestDocumentRepository documentRepository;
    @Mock private ServiceRequestRepository requestRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private DocumentService service;

    private ServiceRequest request;

    @BeforeEach
    void setUp() {
        CitizenProfile citizen = new CitizenProfile("CIT-1", "USR-1", "NID-1", "addr", "W1", "Z1");
        request = new ServiceRequest();
        request.setRequestId("REQ-1");
        request.setCitizen(citizen);
        request.setStatus(RequestStatus.Submitted);
    }

    @Nested
    @DisplayName("uploadDocument")
    class UploadDocument {

        private final MultipartFile file =
                new MockMultipartFile("file", "id.pdf", "application/pdf", "data".getBytes());

        @Test
        @DisplayName("stores the file and persists a Pending document")
        void uploadsSuccessfully() {
            when(requestRepository.findById("REQ-1")).thenReturn(Optional.of(request));
            when(fileStorageService.store(file, "REQ-1", "CIT-1")).thenReturn("REQ-1/CIT-1_REQ-1_x.pdf");

            MessageResponse response = service.uploadDocument("REQ-1", "NationalID", file);

            ArgumentCaptor<RequestDocument> captor = ArgumentCaptor.forClass(RequestDocument.class);
            verify(documentRepository).save(captor.capture());
            RequestDocument saved = captor.getValue();

            assertThat(saved.getDocId()).isNotBlank();
            assertThat(saved.getRequest()).isSameAs(request);
            assertThat(saved.getDocumentType()).isEqualTo("NationalID");
            assertThat(saved.getFilePath()).isEqualTo("REQ-1/CIT-1_REQ-1_x.pdf");
            assertThat(saved.getVerificationStatus()).isEqualTo(VerificationStatus.Pending);
            assertThat(saved.getUploadedDate()).isNotNull();
            assertThat(response.message()).contains("pending officer verification");
        }

        @Test
        @DisplayName("rejects a blank documentType before touching the repository")
        void blankDocumentType() {
            assertThatThrownBy(() -> service.uploadDocument("REQ-1", "  ", file))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("documentType");

            verify(requestRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("throws when the request does not exist")
        void requestNotFound() {
            when(requestRepository.findById("REQ-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.uploadDocument("REQ-X", "NationalID", file))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(fileStorageService, never()).store(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("rejects uploads to a terminal request")
        void terminalRequest() {
            request.setStatus(RequestStatus.Completed);
            when(requestRepository.findById("REQ-1")).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.uploadDocument("REQ-1", "NationalID", file))
                    .isInstanceOf(UnprocessableEntityException.class);

            verify(fileStorageService, never()).store(any(), anyString(), anyString());
            verify(documentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getDocuments")
    class GetDocuments {

        @Test
        @DisplayName("returns the mapped documents when the request exists")
        void returnsDocuments() {
            RequestDocument doc = new RequestDocument();
            doc.setDocId("DOC-1");
            doc.setDocumentType("NationalID");
            doc.setVerificationStatus(VerificationStatus.Verified);
            doc.setUploadedDate(LocalDateTime.now());

            when(requestRepository.existsById("REQ-1")).thenReturn(true);
            when(documentRepository.findByRequest_RequestId("REQ-1")).thenReturn(List.of(doc));

            List<DocumentItemResponse> result = service.getDocuments("REQ-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).docId()).isEqualTo("DOC-1");
            assertThat(result.get(0).verificationStatus()).isEqualTo(VerificationStatus.Verified);
        }

        @Test
        @DisplayName("throws when the request does not exist")
        void requestNotFound() {
            when(requestRepository.existsById("REQ-X")).thenReturn(false);

            assertThatThrownBy(() -> service.getDocuments("REQ-X"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("verifyDocument")
    class VerifyDocument {

        private RequestDocument document() {
            RequestDocument doc = new RequestDocument();
            doc.setDocId("DOC-1");
            doc.setRequest(request);
            doc.setVerificationStatus(VerificationStatus.Pending);
            return doc;
        }

        @Test
        @DisplayName("marks a document Verified without changing the parent request")
        void verifies() {
            RequestDocument doc = document();
            when(documentRepository.findById("DOC-1")).thenReturn(Optional.of(doc));

            MessageResponse response = service.verifyDocument(
                    "DOC-1", new VerifyDocumentRequest(VerificationStatus.Verified));

            assertThat(doc.getVerificationStatus()).isEqualTo(VerificationStatus.Verified);
            assertThat(response.message()).contains("verified successfully");
            verify(documentRepository).save(doc);
            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejecting a document moves the parent request to PendingDocuments")
        void rejectsAndReopensRequest() {
            RequestDocument doc = document();
            when(documentRepository.findById("DOC-1")).thenReturn(Optional.of(doc));

            MessageResponse response = service.verifyDocument(
                    "DOC-1", new VerifyDocumentRequest(VerificationStatus.Rejected));

            assertThat(doc.getVerificationStatus()).isEqualTo(VerificationStatus.Rejected);
            assertThat(request.getStatus()).isEqualTo(RequestStatus.PendingDocuments);
            assertThat(response.message()).contains("PendingDocuments");
            verify(documentRepository).save(doc);
            verify(requestRepository).save(request);
        }

        @Test
        @DisplayName("rejects a null verification status")
        void nullStatus() {
            assertThatThrownBy(() -> service.verifyDocument(
                    "DOC-1", new VerifyDocumentRequest(null)))
                    .isInstanceOf(BadRequestException.class);

            verify(documentRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("rejects setting the status back to Pending")
        void pendingStatus() {
            assertThatThrownBy(() -> service.verifyDocument(
                    "DOC-1", new VerifyDocumentRequest(VerificationStatus.Pending)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("throws when the document does not exist")
        void documentNotFound() {
            when(documentRepository.findById(eq("DOC-X"))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyDocument(
                    "DOC-X", new VerifyDocumentRequest(VerificationStatus.Verified)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
