package com.civicdesk.module.iam.service;

import com.civicdesk.module.iam.dto.response.DepartmentResponse;
import com.civicdesk.module.iam.repository.DepartmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll(Sort.by("name")).stream()
                .map(DepartmentResponse::from)
                .toList();
    }
}
