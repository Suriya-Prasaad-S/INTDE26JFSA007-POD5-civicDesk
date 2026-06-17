package com.civicdesk.module.iam.service;

import com.civicdesk.common.exception.AccountInactiveException;
import com.civicdesk.common.exception.AccountSuspendedException;
import com.civicdesk.common.exception.BadCredentialsException;
import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.DuplicateEmailException;
import com.civicdesk.common.exception.ForbiddenException;
import com.civicdesk.common.exception.PasswordNotSetException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.util.JwtUtil;
import com.civicdesk.module.iam.dto.request.CitizenLoginRequest;
import com.civicdesk.module.iam.dto.request.RegisterRequest;
import com.civicdesk.module.iam.dto.request.SetPasswordRequest;
import com.civicdesk.module.iam.dto.request.StaffLoginRequest;
import com.civicdesk.module.auditlog.enums.AuditAction;
import com.civicdesk.module.auditlog.enums.AuditModule;
import com.civicdesk.module.auditlog.service.AuditService;
import com.civicdesk.module.iam.dto.response.AuthResponse;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    @Value("${app.jwt.expiry}")
    private long expiry;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void register(RegisterRequest req, String ip) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException("Email already registered");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setPasswordSet(true); 
        user.setPhone(req.getPhone());
        user.setRole(Role.CIT.name());
        user.setStatus(UserStatus.ACT.getLabel());
        userRepository.save(user);

        auditService.log(user.getUserId(), AuditAction.REGISTER.name(), AuditModule.IAM.name(), ip);
    }

    @Override
    public AuthResponse citizenLogin(CitizenLoginRequest req, String ip) {
        User user = authenticate(req.getEmail(), req.getPassword());
        if (!user.getRole().equals(Role.CIT.name())) {
            throw new ForbiddenException("Please use the staff portal");
        }
        return issueToken(user, ip);
    }

    @Override
    public AuthResponse staffLogin(StaffLoginRequest req, String ip) {
        User user = authenticate(req.getEmail(), req.getPassword());
        if (user.getRole().equals(Role.CIT.name())) {
            throw new ForbiddenException("Please use the citizen portal");
        }
        return issueToken(user, ip);
    }

   
    private User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        
        if (!user.isPasswordSet()) {
            throw new PasswordNotSetException("Please set your password before logging in");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }


        String status = UserStatus.normalize(user.getStatus());
        if (UserStatus.SUS.getLabel().equals(status)) {
            throw new AccountSuspendedException("Account suspended contact admin");
        }
        if (!UserStatus.ACT.getLabel().equals(status)) {
            throw new AccountInactiveException("Your account is inactive. Please contact your administrator to reactivate it.");
        }
        return user;
    }

    @Override
    @Transactional
    public void setPassword(SetPasswordRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isPasswordSet()) {
            throw new ForbiddenException("Password already set. Use forgot password to reset.");
        }
        if (req.getNewPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordSet(true);
        userRepository.save(user);

        auditService.log(user.getUserId(), AuditAction.SET_PASSWORD.name(), AuditModule.IAM.name(), "SYSTEM");
    }

    private AuthResponse issueToken(User user, String ip) {
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole());
        auditService.log(user.getUserId(), AuditAction.LOGIN.name(), AuditModule.IAM.name(), ip);
        return new AuthResponse(token, user.getUserId(), user.getRole(), expiry / 1000);
    }
}
