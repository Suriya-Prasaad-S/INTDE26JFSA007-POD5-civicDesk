package com.civicdesk.module.iam.service;

import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.iam.dto.request.CreateUserRequest;
import com.civicdesk.module.iam.dto.response.UserResponse;

public interface UserService {

    UserResponse getById(String userId);

    UserResponse createUser(CreateUserRequest req, String callerRole, String callerUserId);

    UserResponse updateStatus(String userId, String status);

    PageResponse<UserResponse> getUsers(String callerRole, String callerUserId,
                                        String roleFilter, String statusFilter, String departmentIdFilter,
                                        int page, int size);
}
