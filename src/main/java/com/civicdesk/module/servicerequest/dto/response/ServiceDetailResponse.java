package com.civicdesk.module.serviceRequest.dto.response;

import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full details of a single catalog service, returned by getService.
 */
public record ServiceDetailResponse(
        String serviceId,
        String serviceName,
        String departmentId,
        ServiceCategory category,
        int processingDays,
        List<String> requiredDocuments,
        BigDecimal fee,
        ServiceStatus status
) {
}
