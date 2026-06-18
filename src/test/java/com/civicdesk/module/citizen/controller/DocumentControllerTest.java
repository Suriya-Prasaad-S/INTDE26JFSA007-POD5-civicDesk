package com.civicdesk.module.citizen.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.civicdesk.module.citizen.dto.response.DocumentDetailResponse;
import com.civicdesk.module.citizen.dto.response.DocumentSummaryResponse;
import com.civicdesk.module.citizen.service.DocumentService;
import com.civicdesk.module.citizen.support.FileStorageService;
import com.civicdesk.module.iam.security.JwtAuthFilter;

/**
 * Web-layer coverage for the citizen document-wallet read endpoints: listings and the raw byte
 * download. The on-disk {@link FileStorageService} is mocked.
 */
@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;
    @MockitoBean
    private FileStorageService fileStorage;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void getAllDocuments_returns200_withDataArray() throws Exception {
        when(documentService.getAllDocuments("cit-1")).thenReturn(List.of(summary()));

        mockMvc.perform(get("/citizenProfile/cit-1/getAllDocuments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentId").value("50000001"))
                .andExpect(jsonPath("$.data[0].status").value("V"));
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void getDocumentById_returns200_withDetail() throws Exception {
        when(documentService.getDocumentById("cit-1", "50000001")).thenReturn(detail());

        mockMvc.perform(get("/citizenProfile/cit-1/getDocumentById/50000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value("50000001"))
                .andExpect(jsonPath("$.data.citizenId").value("cit-1"));
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void downloadDocumentFile_streamsBytes_withInlineDisposition() throws Exception {
        Resource resource = new ByteArrayResource("pdf-bytes".getBytes());
        when(documentService.resolveDownloadFileName("cit-1", "50000001")).thenReturn("abc.pdf");
        when(fileStorage.load("abc.pdf")).thenReturn(resource);

        mockMvc.perform(get("/citizenProfile/cit-1/documents/50000001/file"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"abc.pdf\""))
                .andExpect(content().bytes("pdf-bytes".getBytes()));
    }

    private DocumentSummaryResponse summary() {
        return new DocumentSummaryResponse(
                "50000001", "NationalID", "id.pdf", "pdf", 2, "V",
                LocalDate.of(2024, 1, 1), null, null, LocalDateTime.now());
    }

    private DocumentDetailResponse detail() {
        return new DocumentDetailResponse(
                "50000001", "cit-1", "NationalID", "id.pdf",
                "abc.pdf", "pdf", 2,
                LocalDate.of(2024, 1, 1), null, "V", null, null, LocalDateTime.now());
    }
}
