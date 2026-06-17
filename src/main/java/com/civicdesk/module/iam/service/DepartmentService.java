package com.civicdesk.module.iam.service;

import com.civicdesk.module.iam.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    List<DepartmentResponse> getAll();
}
