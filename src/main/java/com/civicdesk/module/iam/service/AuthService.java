package com.civicdesk.module.iam.service;

import com.civicdesk.module.iam.dto.request.CitizenLoginRequest;
import com.civicdesk.module.iam.dto.request.RegisterRequest;
import com.civicdesk.module.iam.dto.request.SetPasswordRequest;
import com.civicdesk.module.iam.dto.request.StaffLoginRequest;
import com.civicdesk.module.iam.dto.response.AuthResponse;

public interface AuthService {

    void register(RegisterRequest req, String ip);

    AuthResponse citizenLogin(CitizenLoginRequest req, String ip);

    AuthResponse staffLogin(StaffLoginRequest req, String ip);

    void setPassword(SetPasswordRequest req);
}
