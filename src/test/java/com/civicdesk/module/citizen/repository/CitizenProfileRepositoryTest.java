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

import com.civicdesk.module.citizen.entity.CitizenProfile;
import com.civicdesk.module.citizen.entity.enums.CitizenStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
// Own isolated in-memory DB so this slice never shares (or drops) the seeded
// `civicdesk_test` schema that the @SpringBootTest integration tests rely on.
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:citizen_profile_repo_test;DB_CLOSE_DELAY=-1")
class CitizenProfileRepositoryTest {

    @Autowired
    private CitizenProfileRepository repository;

    @BeforeEach
    void setup() {
        repository.save(profile("u1", CitizenStatus.Active, "Ward 12", "hash-0000000001"));
        repository.save(profile("u2", CitizenStatus.Active, "Ward 12", "hash-0000000002"));
        repository.save(profile("u3", CitizenStatus.Verified, "Ward 99", "hash-0000000003"));
    }

    @Test
    void saveAndFindById_usesUserIdAsSharedPrimaryKey() {
        Optional<CitizenProfile> found = repository.findById("u1");
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("u1");
        assertThat(found.get().getStatus()).isEqualTo(CitizenStatus.Active);
    }

    @Test
    void existsByNationalIdHash_knownId_returnsTrue() {
        assertThat(repository.existsByNationalIdHash("hash-0000000001")).isTrue();
    }

    @Test
    void existsByNationalIdHash_unknownId_returnsFalse() {
        assertThat(repository.existsByNationalIdHash("hash-9999999999")).isFalse();
    }

    @Test
    void findByWard_returnsOnlyThatWard() {
        List<CitizenProfile> result = repository.findByWard("Ward 12");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getWard().equals("Ward 12"));
    }

    @Test
    void findByWard_unknownWard_returnsEmpty() {
        assertThat(repository.findByWard("Ward 404")).isEmpty();
    }

    @Test
    void findByStatus_active_returnsOnlyActiveCitizens() {
        List<CitizenProfile> result = repository.findByStatus(CitizenStatus.Active);
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getStatus() == CitizenStatus.Active);
    }

    @Test
    void findByStatus_flagged_returnsEmpty() {
        assertThat(repository.findByStatus(CitizenStatus.Flagged)).isEmpty();
    }

    @Test
    void statusPersistsAsSingleCharacterCode() {
        CitizenProfile reloaded = repository.findById("u3").orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CitizenStatus.Verified);
        assertThat(reloaded.getStatus().getCode()).isEqualTo("V");
    }

    private CitizenProfile profile(String userId, CitizenStatus status, String ward, String nationalIdHash) {
        CitizenProfile p = new CitizenProfile();
        p.setUserId(userId);
        p.setStatus(status);
        p.setWard(ward);
        p.setNationalIdHash(nationalIdHash);
        p.setCreatedBy(userId);
        return p;
    }
}
