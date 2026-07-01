package com.civicdesk.module.serviceRequest.dto.response;

import java.util.List;

/**
 * Analytics payload returned for service request dashboards.
 */
public record ServiceRequestAnalyticsResponse(
        Long totalRequests,
        List<LabelCount> statusBreakdown,
        List<LabelCount> serviceBreakdown,
        List<DateCount> trend,
        Long overdueRequests
) {
}
