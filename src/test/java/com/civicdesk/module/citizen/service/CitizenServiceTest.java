package com.civicdesk.module.citizen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import com.civicdesk.common.exception.citizen.DuplicateResourceException;
import com.civicdesk.common.exception.citizen.InvalidRequestException;
import com.civicdesk.common.exception.citizen.ResourceNotFoundException;
import com.civicdesk.common.util.NationalIdUtil;
import com.civicdesk.module.citizen.dto.request.CompleteCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.UpdateCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.VerifyCitizenRequest;
import com.civicdesk.module.citizen.dto.response.CitizenProfileResponse;
import com.civicdesk.module.citizen.dto.response.CitizenSummaryResponse;
import com.civicdesk.module.citizen.entity.CitizenProfile;
import com.civicdesk.module.citizen.entity.enums.CitizenStatus;
import com.civicdesk.module.citizen.entity.enums.Gender;
import com.civicdesk.module.citizen.repository.CitizenProfileRepository;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.repository.UserRepository;

/** Unit tests for {@link CitizenService}. Identity comes from the security context. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitizenServiceTest {

    private static final String CITIZEN = "cit-1";

    @Mock CitizenProfileRepository citizenRepository;
    @Mock UserRepository userRepository;

    CitizenService service;

    @BeforeEach
    void setup() {
        service = new CitizenService(citizenRepository, userRepository);
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
    // getMyProfile
    // ---------------------------------------------------------------------------------------------

    @Test
    void getMyProfile_existingProfile_returnsResponseWithUserIdentity() {
        CitizenProfile profile = profile(CITIZEN, CitizenStatus.Verified);
        profile.setNationalIdLast4("7890");
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile));
        when(userRepository.findById(CITIZEN)).thenReturn(Optional.of(user(CITIZEN, "Ravi Kumar")));

        CitizenProfileResponse res = service.getMyProfile();

        assertThat(res.userId()).isEqualTo(CITIZEN);
        assertThat(res.name()).isEqualTo("Ravi Kumar");
        assertThat(res.status()).isEqualTo("V");
        // National id is masked, never returned in full.
        assertThat(res.nationalIdNumber()).isEqualTo("****7890");
        verify(citizenRepository, never()).save(any());
    }

    @Test
    void getMyProfile_noProfile_createsActiveStub() {
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.empty());
        when(citizenRepository.save(any(CitizenProfile.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(CITIZEN)).thenReturn(Optional.of(user(CITIZEN, "Ravi Kumar")));

        CitizenProfileResponse res = service.getMyProfile();

        ArgumentCaptor<CitizenProfile> captor = ArgumentCaptor.forClass(CitizenProfile.class);
        verify(citizenRepository).save(captor.capture());
        CitizenProfile saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(CITIZEN);
        assertThat(saved.getStatus()).isEqualTo(CitizenStatus.Active);
        assertThat(saved.getCreatedBy()).isEqualTo(CITIZEN);
        assertThat(res.status()).isEqualTo("A");
    }

    @Test
    void getMyProfile_noUserRow_returnsNullIdentityFields() {
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile(CITIZEN, CitizenStatus.Active)));
        when(userRepository.findById(CITIZEN)).thenReturn(Optional.empty());

        CitizenProfileResponse res = service.getMyProfile();

        assertThat(res.name()).isNull();
        assertThat(res.email()).isNull();
        assertThat(res.phone()).isNull();
    }

    @Test
    void getMyProfile_noAuthenticatedUser_throwsInvalidRequest() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.getMyProfile())
                .isInstanceOf(InvalidRequestException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // completeProfile
    // ---------------------------------------------------------------------------------------------

    @Test
    void completeProfile_whenVerified_setsFieldsAndSaves() {
        CitizenProfile profile = profile(CITIZEN, CitizenStatus.Verified);
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile));
        when(citizenRepository.existsByNationalIdHash(any())).thenReturn(false);
        when(citizenRepository.save(any(CitizenProfile.class))).thenAnswer(i -> i.getArgument(0));

        service.completeProfile(completeReq());

        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(profile.getGender()).isEqualTo(Gender.Male);
        // Raw national id is never stored — only its hash + last 4 digits.
        assertThat(profile.getNationalIdHash()).isEqualTo(NationalIdUtil.hash("IND1234567890"));
        assertThat(profile.getNationalIdLast4()).isEqualTo("7890");
        assertThat(profile.getAddress()).isEqualTo("12 Main St");
        assertThat(profile.getWard()).isEqualTo("Ward 12");
        assertThat(profile.getZone()).isEqualTo("Zone A");
        verify(citizenRepository).save(profile);
    }

    @Test
    void completeProfile_genderIsCaseInsensitive() {
        CitizenProfile profile = profile(CITIZEN, CitizenStatus.Verified);
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile));
        when(citizenRepository.existsByNationalIdHash(any())).thenReturn(false);
        when(citizenRepository.save(any(CitizenProfile.class))).thenAnswer(i -> i.getArgument(0));

        service.completeProfile(new CompleteCitizenProfileRequest(
                LocalDate.of(1990, 1, 1), "fEmAlE", "IND1234567890", "12 Main St", "Ward 12", null));

        assertThat(profile.getGender()).isEqualTo(Gender.Female);
    }

    @Test
    void completeProfile_noProfile_throwsBusinessRule() {
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.completeProfile(completeReq()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void completeProfile_notVerified_throwsBusinessRule() {
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile(CITIZEN, CitizenStatus.Active)));
        assertThatThrownBy(() -> service.completeProfile(completeReq()))
                .isInstanceOf(BusinessRuleException.class);
        verify(citizenRepository, never()).save(any());
    }

    @Test
    void completeProfile_duplicateNationalId_throwsDuplicateResource() {
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile(CITIZEN, CitizenStatus.Verified)));
        when(citizenRepository.existsByNationalIdHash(any())).thenReturn(true);
        assertThatThrownBy(() -> service.completeProfile(completeReq()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(citizenRepository, never()).save(any());
    }

    @Test
    void completeProfile_invalidGender_throwsInvalidRequest() {
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile(CITIZEN, CitizenStatus.Verified)));
        when(citizenRepository.existsByNationalIdHash(any())).thenReturn(false);
        CompleteCitizenProfileRequest req = new CompleteCitizenProfileRequest(
                LocalDate.of(1990, 1, 1), "Martian", "IND1234567890", "12 Main St", "Ward 12", null);
        assertThatThrownBy(() -> service.completeProfile(req))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // updateMyProfile
    // ---------------------------------------------------------------------------------------------

    @Test
    void updateMyProfile_updatesOnlyProvidedFields() {
        CitizenProfile profile = profile(CITIZEN, CitizenStatus.Verified);
        profile.setAddress("old address");
        profile.setWard("old ward");
        profile.setZone("old zone");
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile));
        when(citizenRepository.save(any(CitizenProfile.class))).thenAnswer(i -> i.getArgument(0));

        service.updateMyProfile(new UpdateCitizenProfileRequest("new address", null, null));

        assertThat(profile.getAddress()).isEqualTo("new address");
        assertThat(profile.getWard()).isEqualTo("old ward"); // untouched
        assertThat(profile.getZone()).isEqualTo("old zone"); // untouched
        verify(citizenRepository).save(profile);
    }

    @Test
    void updateMyProfile_noFields_throwsInvalidRequest() {
        assertThatThrownBy(() -> service.updateMyProfile(new UpdateCitizenProfileRequest(null, null, null)))
                .isInstanceOf(InvalidRequestException.class);
        verify(citizenRepository, never()).findById(any());
    }

    @Test
    void updateMyProfile_notFound_throwsResourceNotFound() {
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateMyProfile(new UpdateCitizenProfileRequest("addr", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMyProfile_notVerified_throwsBusinessRule() {
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile(CITIZEN, CitizenStatus.Active)));
        assertThatThrownBy(() -> service.updateMyProfile(new UpdateCitizenProfileRequest("addr", null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // getPendingVerifications / listings
    // ---------------------------------------------------------------------------------------------

    @Test
    void getPendingVerifications_returnsActiveCitizenSummaries() {
        when(citizenRepository.findByStatus(CitizenStatus.Active))
                .thenReturn(List.of(profile("c1", CitizenStatus.Active), profile("c2", CitizenStatus.Active)));
        when(userRepository.findAllById(anyList()))
                .thenReturn(List.of(user("c1", "Alice"), user("c2", "Bob")));

        List<CitizenSummaryResponse> result = service.getPendingVerifications();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CitizenSummaryResponse::name).contains("Alice", "Bob");
        assertThat(result).allMatch(s -> s.status().equals("A"));
    }

    @Test
    void getCitizensByWard_returnsSummariesForThatWard() {
        CitizenProfile p = profile("c1", CitizenStatus.Verified);
        p.setWard("Ward 12");
        when(citizenRepository.findByWard("Ward 12")).thenReturn(List.of(p));
        when(userRepository.findAllById(anyList())).thenReturn(List.of(user("c1", "Alice")));

        List<CitizenSummaryResponse> result = service.getCitizensByWard("Ward 12");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ward()).isEqualTo("Ward 12");
        assertThat(result.get(0).name()).isEqualTo("Alice");
    }

    @Test
    void getAllCitizens_returnsSummaries() {
        when(citizenRepository.findAll()).thenReturn(List.of(profile("c1", CitizenStatus.Verified)));
        when(userRepository.findAllById(anyList())).thenReturn(List.of(user("c1", "Alice")));

        assertThat(service.getAllCitizens()).hasSize(1);
    }

    @Test
    void getAllCitizens_emptyRepository_returnsEmpty() {
        when(citizenRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAllById(anyList())).thenReturn(List.of());
        assertThat(service.getAllCitizens()).isEmpty();
    }

    @Test
    void summaries_missingUserRow_yieldNullName() {
        when(citizenRepository.findAll()).thenReturn(List.of(profile("c1", CitizenStatus.Verified)));
        when(userRepository.findAllById(anyList())).thenReturn(List.of()); // no matching user

        List<CitizenSummaryResponse> result = service.getAllCitizens();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isNull();
    }

    // ---------------------------------------------------------------------------------------------
    // verifyCitizen
    // ---------------------------------------------------------------------------------------------

    @Test
    void verifyCitizen_activeToVerified_savesAndStampsVerifier() {
        authenticateAs("officer-1", "FO");
        CitizenProfile profile = profile("c1", CitizenStatus.Active);
        when(citizenRepository.findById("c1")).thenReturn(Optional.of(profile));
        when(citizenRepository.save(any(CitizenProfile.class))).thenAnswer(i -> i.getArgument(0));

        service.verifyCitizen("c1", new VerifyCitizenRequest("V"));

        assertThat(profile.getStatus()).isEqualTo(CitizenStatus.Verified);
        assertThat(profile.getVerifiedBy()).isEqualTo("officer-1");
        assertThat(profile.getVerifiedAt()).isNotNull();
        verify(citizenRepository).save(profile);
    }

    @Test
    void verifyCitizen_activeToFlagged_allowed() {
        authenticateAs("officer-1", "FO");
        CitizenProfile profile = profile("c1", CitizenStatus.Active);
        when(citizenRepository.findById("c1")).thenReturn(Optional.of(profile));
        when(citizenRepository.save(any(CitizenProfile.class))).thenAnswer(i -> i.getArgument(0));

        service.verifyCitizen("c1", new VerifyCitizenRequest("F"));

        assertThat(profile.getStatus()).isEqualTo(CitizenStatus.Flagged);
    }

    @Test
    void verifyCitizen_flaggedBackToActive_isNotAValidVerifyTarget() {
        // 'A' is rejected as a verify target before any transition check.
        CitizenProfile profile = profile("c1", CitizenStatus.Flagged);
        when(citizenRepository.findById("c1")).thenReturn(Optional.of(profile));
        assertThatThrownBy(() -> service.verifyCitizen("c1", new VerifyCitizenRequest("A")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyCitizen_invalidStatusCode_throwsInvalidRequest() {
        assertThatThrownBy(() -> service.verifyCitizen("c1", new VerifyCitizenRequest("X")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verifyCitizen_notFound_throwsResourceNotFound() {
        when(citizenRepository.findById("c1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifyCitizen("c1", new VerifyCitizenRequest("V")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifyCitizen_illegalTransition_verifiedToVerified_throwsBusinessRule() {
        CitizenProfile profile = profile("c1", CitizenStatus.Verified);
        when(citizenRepository.findById("c1")).thenReturn(Optional.of(profile));
        assertThatThrownBy(() -> service.verifyCitizen("c1", new VerifyCitizenRequest("V")))
                .isInstanceOf(BusinessRuleException.class);
        verify(citizenRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------------------------------
    // fixtures
    // ---------------------------------------------------------------------------------------------

    private CompleteCitizenProfileRequest completeReq() {
        return new CompleteCitizenProfileRequest(
                LocalDate.of(1990, 1, 1), "Male", "IND1234567890", "12 Main St", "Ward 12", "Zone A");
    }

    private CitizenProfile profile(String userId, CitizenStatus status) {
        CitizenProfile p = new CitizenProfile();
        p.setUserId(userId);
        p.setStatus(status);
        return p;
    }

    private User user(String userId, String name) {
        User u = new User();
        u.setUserId(userId);
        u.setName(name);
        u.setEmail(name.toLowerCase() + "@example.com");
        u.setPhone("9000000000");
        return u;
    }
}
