package com.civicdesk.module.iam.service;

import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.DuplicateEmailException;
import com.civicdesk.common.exception.ForbiddenException;
import com.civicdesk.module.iam.dto.request.CreateUserRequest;
import com.civicdesk.module.iam.dto.response.UserResponse;
import com.civicdesk.module.iam.entity.Department;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.repository.DepartmentRepository;
import com.civicdesk.module.iam.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private CreateUserRequest req(String role, String deptId) {
        CreateUserRequest r = new CreateUserRequest();
        r.setName("New User");
        r.setEmail("new@civicdesk.gov");
        r.setRole(role);
        r.setDepartmentId(deptId);
        return r;
    }

    private Department dept(String id) {
        Department d = new Department("Infrastructure");
        d.setDepartmentId(id);
        return d;
    }

    @Test
    void adminCreatesSupervisor_succeeds_andMapsDepartmentSupervisor() {
        Department department = dept("dept-1");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(departmentRepository.findById("dept-1")).thenReturn(Optional.of(department));

        UserResponse res = userService.createUser(req("DS", "dept-1"), "ADM", "admin-id");

        assertEquals("DS", res.getRole());
        assertEquals("dept-1", res.getDepartmentId());
        // department now points back at the new supervisor (one-to-one mapping)
        assertEquals(res.getUserId(), department.getDepartmentSupervisorId());
    }

    @Test
    void adminCreatesNonSupervisor_throwsForbidden() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> userService.createUser(req("ENG", "dept-1"), "ADM", "admin-id"));
    }

    @Test
    void adminCreatesSupervisorWithoutDept_throwsBadRequest() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        assertThrows(BadRequestException.class,
                () -> userService.createUser(req("DS", null), "ADM", "admin-id"));
    }

    @Test
    void adminCreatesSupervisor_departmentNotFound_throwsBadRequest() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(departmentRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class,
                () -> userService.createUser(req("DS", "missing"), "ADM", "admin-id"));
    }

    @Test
    void supervisorCreatesEngineer_assignsOwnDepartment() {
        User supervisor = new User();
        supervisor.setUserId("sup-id");
        supervisor.setDepartmentId("dept-9");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findById("sup-id")).thenReturn(Optional.of(supervisor));

        // departmentId in body is ignored; owner's department is used.
        UserResponse res = userService.createUser(req("ENG", "ignored"), "DS", "sup-id");

        assertEquals("ENG", res.getRole());
        assertEquals("dept-9", res.getDepartmentId());
    }

    @Test
    void supervisorCreatesAdmin_throwsForbidden() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> userService.createUser(req("ADM", null), "DS", "sup-id"));
    }

    @Test
    void createUser_duplicateEmail_throwsDuplicate() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        assertThrows(DuplicateEmailException.class,
                () -> userService.createUser(req("DS", "dept-1"), "ADM", "admin-id"));
    }
}
