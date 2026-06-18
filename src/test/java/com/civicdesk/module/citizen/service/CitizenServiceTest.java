package com.civicdesk.module.citizen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
import com.civicdesk.common.exception.citizen.ForbiddenActionException;
import com.civicdesk.common.exception.citizen.InvalidRequestException;
import com.civicdesk.common.exception.citizen.ResourceNotFoundException;
import com.civicdesk.common.util.NationalIdUtil;
import com.civicdesk.module.citizen.dto.request.CitizenRegistrationRequest;
import com.civicdesk.module.citizen.dto.request.UpdateCitizenProfileRequest;
import com.civicdesk.module.citizen.dto.request.VerifyCitizenRequest;
import com.civicdesk.module.citizen.dto.response.CitizenProfileResponse;
import com.civicdesk.module.citizen.dto.response.CitizenSummaryResponse;
import com.civicdesk.module.citizen.entity.CitizenProfile;
import com.civicdesk.module.citizen.entity.enums.CitizenStatus;
import com.civicdesk.module.citizen.entity.enums.Gender;
import com.civicdesk.module.citizen.repository.CitizenProfileRepository;
import com.civicdesk.module.iam.dto.request.RegisterRequest;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.repository.UserRepository;
import com.civicdesk.module.iam.service.AuthService;

/** Unit tests for {@link CitizenService}. Identity comes from the security context. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitizenServiceTest {

    private static final String CITIZEN = "cit-1";

    @Mock CitizenProfileRepository citizenRepository;
    @Mock UserRepository userRepository;
    @Mock AuthService authService;

    CitizenService service;

    @BeforeEach
    void setup() {
        service = new CitizenService(citizenRepository, userRepository, authService);
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
    // registerCitizen
    // ---------------------------------------------------------------------------------------------

    @Test
    void registerCitizen_createsUserViaIamThenProfile() {
        when(citizenRepository.existsByNationalIdHash(any())).thenReturn(false);
        when(userRepository.findByEmail("ravi@example.com"))
                .thenReturn(Optional.of(user("10000050", "Ravi Kumar")));
        when(citizenRepository.save(any(CitizenProfile.class))).thenAnswer(i -> i.getArgument(0));

        service.registerCitizen(registrationReq(), "proof.pdf", "1.2.3.4");

        verify(authService).register(any(RegisterRequest.class), eq("1.2.3.4"));
        ArgumentCaptor<CitizenProfile> captor = ArgumentCaptor.forClass(CitizenProfile.class);
        verify(citizenRepository).save(captor.capture());
        CitizenProfile saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("10000050");
        assertThat(saved.getStatus()).isEqualTo(CitizenStatus.Active);
        assertThat(saved.getUserProof()).isEqualTo("proof.pdf");
        assertThat(saved.getGender()).isEqualTo(Gender.Male);
        assertThat(saved.getNationalIdLast4()).isEqualTo("7890");
        // Raw national id is never stored — only its hash.
        assertThat(saved.getNationalIdHash()).isEqualTo(NationalIdUtil.hash("IND1234567890"));
    }

    @Test
    void registerCitizen_duplicateNationalId_throwsBeforeCreatingUser() {
        when(citizenRepository.existsByNationalIdHash(any())).thenReturn(true);

        assertThatThrownBy(() -> service.registerCitizen(registrationReq(), "proof.pdf", "ip"))
                .isInstanceOf(DuplicateResourceException.class);
        verify(authService, never()).register(any(), any());
        verify(citizenRepository, never()).save(any());
    }

    @Test
    void registerCitizen_invalidGender_throwsInvalidRequest() {
        when(citizenRepository.existsByNationalIdHash(any())).thenReturn(false);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user("10000050", "Ravi")));
        CitizenRegistrationRequest req = registrationReq();
        req.setGender("Martian");

        assertThatThrownBy(() -> service.registerCitizen(req, "proof.pdf", "ip"))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // getMyProfile (read-only)
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
        assertThat(res.nationalIdNumber()).isEqualTo("****7890");
    }

    @Test
    void getMyProfile_noProfile_throwsResourceNotFound() {
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMyProfile())
                .isInstanceOf(ResourceNotFoundException.class);
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
        assertThat(profile.getWard()).isEqualTo("old ward");
        assertThat(profile.getZone()).isEqualTo("old zone");
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

    // ---------------------------------------------------------------------------------------------
    // resolveProofFileName
    // ---------------------------------------------------------------------------------------------

    @Test
    void resolveProofFileName_owner_returnsStoredName() {
        CitizenProfile profile = profile(CITIZEN, CitizenStatus.Active);
        profile.setUserProof("proof-abc.pdf");
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile));

        assertThat(service.resolveProofFileName(CITIZEN)).isEqualTo("proof-abc.pdf");
    }

    @Test
    void resolveProofFileName_officer_allowed() {
        authenticateAs("officer-1", "DS");
        CitizenProfile profile = profile("c9", CitizenStatus.Active);
        profile.setUserProof("proof-c9.pdf");
        when(citizenRepository.findById("c9")).thenReturn(Optional.of(profile));

        assertThat(service.resolveProofFileName("c9")).isEqualTo("proof-c9.pdf");
    }

    @Test
    void resolveProofFileName_otherCitizen_throwsForbidden() {
        // Authenticated as CITIZEN (cit-1) but requesting c9's proof.
        assertThatThrownBy(() -> service.resolveProofFileName("c9"))
                .isInstanceOf(ForbiddenActionException.class);
        verify(citizenRepository, never()).findById(any());
    }

    @Test
    void resolveProofFileName_noProofOnFile_throwsResourceNotFound() {
        CitizenProfile profile = profile(CITIZEN, CitizenStatus.Active); // userProof null
        when(citizenRepository.findById(CITIZEN)).thenReturn(Optional.of(profile));
        assertThatThrownBy(() -> service.resolveProofFileName(CITIZEN))
                .isInstanceOf(ResourceNotFoundException.class);
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
    void summaries_missingUserRow_yieldNullName() {
        when(citizenRepository.findAll()).thenReturn(List.of(profile("c1", CitizenStatus.Verified)));
        when(userRepository.findAllById(anyList())).thenReturn(List.of());

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
    void verifyCitizen_activeTargetRejected_throwsInvalidRequest() {
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

    private CitizenRegistrationRequest registrationReq() {
        CitizenRegistrationRequest r = new CitizenRegistrationRequest();
        r.setName("Ravi Kumar");
        r.setEmail("ravi@example.com");
        r.setPassword("Ravi@1234");
        r.setPhone("9876543210");
        r.setDateOfBirth(LocalDate.of(1990, 1, 1));
        r.setGender("Male");
        r.setNationalIdNumber("IND1234567890");
        r.setAddress("12 Main St");
        r.setWard("Ward 12");
        r.setZone("Zone A");
        return r;
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
