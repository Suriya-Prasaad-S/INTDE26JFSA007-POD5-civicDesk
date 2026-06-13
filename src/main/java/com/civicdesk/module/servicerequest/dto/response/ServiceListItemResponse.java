package com.civicdesk.module.serviceRequest.dto.response;

import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;

import java.math.BigDecimal;

/**
 * A catalog service as listed by getAllServices (summary view, no required-documents list).
 */
public record ServiceListItemResponse(
        String serviceId,
        String serviceName,
        String departmentId,
        ServiceCategory category,
        int processingDays,
        BigDecimal fee,
        ServiceStatus status
) {
}
