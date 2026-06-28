package com.civicdesk.module.iam.service;

import com.civicdesk.common.exception.AccountSuspendedException;
import com.civicdesk.common.exception.BadCredentialsException;
import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.DuplicateEmailException;
import com.civicdesk.common.exception.ForbiddenException;
import com.civicdesk.common.exception.PasswordNotSetException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.util.JwtUtil;
import com.civicdesk.module.auditlog.service.AuditService;
import com.civicdesk.module.iam.dto.request.CitizenLoginRequest;
import com.civicdesk.module.iam.dto.request.RegisterRequest;
import com.civicdesk.module.iam.dto.request.SetPasswordRequest;
import com.civicdesk.module.iam.dto.request.StaffLoginRequest;
import com.civicdesk.module.iam.dto.response.AuthResponse;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeCitizen() {
        User user = new User();
        user.setUserId("test-id");
        user.setEmail("ravi@example.com");
        user.setRole("CIT");
        user.setPasswordHash("hashedpassword");
        user.setPasswordSet(true);
        user.setStatus("A");
        return user;
    }

    @Test
    void citizenLogin_validCredentials_returnsToken() {
        when(userRepository.findByEmail("ravi@example.com")).thenReturn(Optional.of(activeCitizen()));
        when(passwordEncoder.matches("Ravi@1234", "hashedpassword")).thenReturn(true);
        when(jwtUtil.generateToken(any(), any())).thenReturn("mock-jwt-token");

        AuthResponse result = authService.citizenLogin(new CitizenLoginRequest("ravi@example.com", "Ravi@1234"), "127.0.0.1");

        assertNotNull(result);
        assertEquals("mock-jwt-token", result.getToken());
        assertEquals("CIT", result.getRole());
    }

    @Test
    void citizenLogin_wrongPassword_throwsBadCredentials() {
        when(userRepository.findByEmail("ravi@example.com")).thenReturn(Optional.of(activeCitizen()));
        when(passwordEncoder.matches("wrongpass", "hashedpassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.citizenLogin(new CitizenLoginRequest("ravi@example.com", "wrongpass"), "127.0.0.1"));
    }

    @Test
    void staffLogin_passwordNotSet_throwsPasswordNotSet() {
        User user = new User();
        user.setEmail("priya.fo@civicdesk.gov");
        user.setRole("FO");
        user.setPasswordSet(false); // admin-created, never set a password

        when(userRepository.findByEmail("priya.fo@civicdesk.gov")).thenReturn(Optional.of(user));

        assertThrows(PasswordNotSetException.class,
                () -> authService.staffLogin(new StaffLoginRequest("priya.fo@civicdesk.gov", "whatever"), "127.0.0.1"));
    }

    @Test
    void citizenLogin_staffTriesCitizenPortal_throwsForbidden() {
        User user = new User();
        user.setEmail("admin@civicdesk.gov");
        user.setRole("ADM");
        user.setPasswordHash("hashedpassword");
        user.setPasswordSet(true);

        when(userRepository.findByEmail("admin@civicdesk.gov")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThrows(ForbiddenException.class,
                () -> authService.citizenLogin(new CitizenLoginRequest("admin@civicdesk.gov", "Admin@1234"), "127.0.0.1"));
    }

    @Test
    void staffLogin_citizenTriesStaffPortal_throwsForbidden() {
        when(userRepository.findByEmail("ravi@example.com")).thenReturn(Optional.of(activeCitizen()));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThrows(ForbiddenException.class,
                () -> authService.staffLogin(new StaffLoginRequest("ravi@example.com", "Ravi@1234"), "127.0.0.1"));
    }

    @Test
    void staffLogin_suspendedAccount_throwsAccountSuspended() {
        User user = new User();
        user.setEmail("meena@civicdesk.gov");
        user.setRole("DS");
        user.setPasswordHash("hashedpassword");
        user.setPasswordSet(true);
        user.setStatus("S");

        when(userRepository.findByEmail("meena@civicdesk.gov")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThrows(AccountSuspendedException.class,
                () -> authService.staffLogin(new StaffLoginRequest("meena@civicdesk.gov", "Sup@1234"), "127.0.0.1"));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateEmail() {
        when(userRepository.existsByEmail("ravi@example.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("ravi@example.com");
        req.setPassword("Ravi@1234");

        assertThrows(DuplicateEmailException.class,
                () -> authService.register(req, "127.0.0.1"));
    }

    // ---- set-password ----

    private SetPasswordRequest setReq(String email, String pw) {
        SetPasswordRequest r = new SetPasswordRequest();
        r.setEmail(email);
        r.setNewPassword(pw);
        return r;
    }

    @Test
    void setPassword_firstTime_setsHashAndMarksSet() {
        User user = new User();
        user.setUserId("u1");
        user.setEmail("priya.fo@civicdesk.gov");
        user.setPasswordSet(false);

        when(userRepository.findByEmail("priya.fo@civicdesk.gov")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Priya@1234")).thenReturn("bcrypt-hash");

        authService.setPassword(setReq("priya.fo@civicdesk.gov", "Priya@1234"));

        assertTrue(user.isPasswordSet());
        assertEquals("bcrypt-hash", user.getPasswordHash());
    }

    @Test
    void setPassword_alreadySet_throwsForbidden() {
        User user = new User();
        user.setEmail("priya.fo@civicdesk.gov");
        user.setPasswordSet(true);
        when(userRepository.findByEmail("priya.fo@civicdesk.gov")).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class,
                () -> authService.setPassword(setReq("priya.fo@civicdesk.gov", "Priya@1234")));
    }

    @Test
    void setPassword_tooShort_throwsBadRequest() {
        User user = new User();
        user.setEmail("priya.fo@civicdesk.gov");
        user.setPasswordSet(false);
        when(userRepository.findByEmail("priya.fo@civicdesk.gov")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class,
                () -> authService.setPassword(setReq("priya.fo@civicdesk.gov", "short")));
    }

    @Test
    void setPassword_userNotFound_throwsNotFound() {
        when(userRepository.findByEmail("ghost@civicdesk.gov")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.setPassword(setReq("ghost@civicdesk.gov", "Priya@1234")));
    }
}
