package com.civicdesk.module.citizen.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.civicdesk.module.citizen.entity.CitizenDocument;
import com.civicdesk.module.citizen.entity.enums.DocumentStatus;
import com.civicdesk.module.citizen.entity.enums.DocumentType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
// Own isolated in-memory DB so this slice never shares (or drops) the seeded
// `civicdesk_test` schema that the @SpringBootTest integration tests rely on.
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:citizen_document_repo_test;DB_CLOSE_DELAY=-1")
class CitizenDocumentRepositoryTest {

    @Autowired
    private CitizenDocumentRepository repository;

    private String c1Doc1Id;

    @BeforeEach
    void setup() {
        // Citizen c1 owns two documents; citizen c2 owns one.
        c1Doc1Id = repository.save(document("c1", DocumentType.NationalID)).getDocumentId();
        repository.save(document("c1", DocumentType.ResidenceProof));
        repository.save(document("c2", DocumentType.BirthCertificate));
    }

    @Test
    void documentIdIsGenerated() {
        assertThat(c1Doc1Id).isNotBlank();
    }

    @Test
    void findByCitizenId_returnsOnlyThatCitizensDocuments() {
        List<CitizenDocument> result = repository.findByCitizenId("c1");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(d -> d.getCitizenId().equals("c1"));
    }

    @Test
    void findByCitizenId_unknownCitizen_returnsEmpty() {
        assertThat(repository.findByCitizenId("nobody")).isEmpty();
    }

    @Test
    void countByCitizenId_returnsCount() {
        assertThat(repository.countByCitizenId("c1")).isEqualTo(2);
        assertThat(repository.countByCitizenId("c2")).isEqualTo(1);
        assertThat(repository.countByCitizenId("nobody")).isZero();
    }

    @Test
    void findByDocumentIdAndCitizenId_matchingOwner_returnsDocument() {
        Optional<CitizenDocument> found = repository.findByDocumentIdAndCitizenId(c1Doc1Id, "c1");
        assertThat(found).isPresent();
        assertThat(found.get().getDocumentType()).isEqualTo(DocumentType.NationalID);
    }

    @Test
    void findByDocumentIdAndCitizenId_wrongOwner_returnsEmpty() {
        // The document exists, but it is scoped to c1 — c2 must not see it.
        assertThat(repository.findByDocumentIdAndCitizenId(c1Doc1Id, "c2")).isEmpty();
    }

    @Test
    void findByDocumentIdAndCitizenId_unknownDocument_returnsEmpty() {
        assertThat(repository.findByDocumentIdAndCitizenId("99999999", "c1")).isEmpty();
    }

    @Test
    void statusPersistsAsSingleCharacterCode() {
        CitizenDocument reloaded = repository.findByDocumentIdAndCitizenId(c1Doc1Id, "c1").orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.Valid);
        assertThat(reloaded.getStatus().getCode()).isEqualTo("V");
    }

    private CitizenDocument document(String citizenId, DocumentType type) {
        CitizenDocument d = new CitizenDocument();
        d.setCitizenId(citizenId);
        d.setDocumentType(type);
        d.setFileName("file.pdf");
        d.setFilePath("http://localhost/files/file.pdf");
        d.setFileType("pdf");
        d.setFileSizeKb(2);
        d.setStatus(DocumentStatus.Valid);
        return d;
    }
}
