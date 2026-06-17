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
import com.civicdesk.module.iam.repository.spec.UserSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

        
        Department supervisorDepartment = null;

        if (Role.ADM.name().equals(callerRole)) {
            
            if (!Role.DS.name().equals(req.getRole())) {
                throw new ForbiddenException("Admin can only create DS");
            }
            
            if (req.getDepartmentId() == null || req.getDepartmentId().isBlank()) {
                throw new BadRequestException("departmentId is required when creating a DS");
            }
            supervisorDepartment = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new BadRequestException("Department does not exist"));
        } else if (Role.DS.name().equals(callerRole)) {
            
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
    public PageResponse<UserResponse> getUsers(String callerRole, String callerUserId,
                                               String roleFilter, String statusFilter, String departmentIdFilter,
                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<User> spec = Specification.where(null);
        String role = normalizeRoleFilter(roleFilter);
        if (role != null) {
            spec = spec.and(UserSpecifications.hasRole(role));
        }

        if (Role.ADM.name().equals(callerRole)) {
           
            String status = normalizeStatusFilter(statusFilter);
            if (status != null) {
                spec = spec.and(UserSpecifications.hasStatus(status));
            }
           
            if (departmentIdFilter != null && !departmentIdFilter.isBlank()) {
                spec = spec.and(UserSpecifications.inDepartment(departmentIdFilter.trim()));
            }
        } else if (Role.DS.name().equals(callerRole)) {

            String deptId = userRepository.findById(callerUserId)
                    .map(User::getDepartmentId)
                    .orElseThrow(() -> new ForbiddenException("Caller has no department"));
            spec = spec.and(UserSpecifications.inDepartment(deptId))
                    .and(UserSpecifications.hasStatus(UserStatus.ACT.getLabel()));
        } else {
            throw new ForbiddenException("Not allowed to list users");
        }

        Page<User> users = userRepository.findAll(spec, pageable);
        return PageResponse.from(users, UserResponse::from);
    }

 
    private String normalizeRoleFilter(String roleFilter) {
        if (roleFilter == null || roleFilter.isBlank()) {
            return null;
        }
        String candidate = roleFilter.trim().toUpperCase();
        for (Role r : Role.values()) {
            if (r.name().equals(candidate)) {
                return r.name();
            }
        }
        throw new BadRequestException("Invalid role: " + roleFilter);
    }

    
    private String normalizeStatusFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return null;
        }
        String label = UserStatus.normalize(statusFilter);
        if (label == null) {
            throw new BadRequestException("Invalid status: " + statusFilter);
        }
        return label;
    }
}
