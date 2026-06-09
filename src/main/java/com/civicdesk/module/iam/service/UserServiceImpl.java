package com.civicdesk.module.iam.service;

import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.DuplicateEmailException;
import com.civicdesk.common.exception.ForbiddenException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.iam.dto.request.CreateUserRequest;
import com.civicdesk.module.iam.dto.response.UserResponse;
import com.civicdesk.module.iam.entity.Department;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.DepartmentRepository;
import com.civicdesk.module.iam.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final List<String> SUPERVISOR_CREATABLE_ROLES = List.of(
            Role.FO.name(),
            Role.ENG.name(),
            Role.CO.name());

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public UserServiceImpl(UserRepository userRepository, DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public UserResponse getById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest req, String callerRole, String callerUserId) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }

        // Department the new supervisor will head (only set on the ADM path).
        Department supervisorDepartment = null;

        if (Role.ADM.name().equals(callerRole)) {
            // Admin may only mint department supervisors.
            if (!Role.DS.name().equals(req.getRole())) {
                throw new ForbiddenException("Admin can only create DS");
            }
            // A supervisor must be tied to a department that actually exists.
            if (req.getDepartmentId() == null || req.getDepartmentId().isBlank()) {
                throw new BadRequestException("departmentId is required when creating a DS");
            }
            supervisorDepartment = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new BadRequestException("Department does not exist"));
        } else if (Role.DS.name().equals(callerRole)) {
            // Supervisor may only create field staff, always within their own department.
            if (!SUPERVISOR_CREATABLE_ROLES.contains(req.getRole())) {
                throw new ForbiddenException("Supervisor cannot create " + req.getRole());
            }
            String callerDeptId = userRepository.findById(callerUserId)
                    .map(User::getDepartmentId)
                    .orElseThrow(() -> new ForbiddenException("Caller has no department"));
            req.setDepartmentId(callerDeptId);
        } else {
            throw new ForbiddenException("Not allowed to create users");
        }

        // Admin-created staff start with NO password; the owner sets it on first login.
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRole(req.getRole());
        user.setDepartmentId(req.getDepartmentId());
        user.setStatus(UserStatus.ACT.getLabel());
        user.setPasswordHash(null);
        user.setPasswordSet(false);
        userRepository.save(user);

        // One-to-one: point the department back at its newly assigned supervisor.
        if (supervisorDepartment != null) {
            supervisorDepartment.setDepartmentSupervisorId(user.getUserId());
            departmentRepository.save(supervisorDepartment);
        }

        return UserResponse.from(user);
    }

    @Override
    public UserResponse updateStatus(String userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.normalize(status));
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Override
    public PageResponse<UserResponse> getUsers(String callerRole, String callerUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;

        if (Role.ADM.name().equals(callerRole)) {
            // Admin sees every account regardless of status.
            users = userRepository.findAll(pageable);
        } else if (Role.DS.name().equals(callerRole)) {
            // Supervisor sees only active staff within their own department;
            // inactive/suspended accounts are hidden from them.
            String deptId = userRepository.findById(callerUserId)
                    .map(User::getDepartmentId)
                    .orElseThrow(() -> new ForbiddenException("Caller has no department"));
            users = userRepository.findByDepartmentIdAndStatus(deptId, UserStatus.ACT.getLabel(), pageable);
        } else {
            throw new ForbiddenException("Not allowed to list users");
        }

        return PageResponse.from(users, UserResponse::from);
    }
}
