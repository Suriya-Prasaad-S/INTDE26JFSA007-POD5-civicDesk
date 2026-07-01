package com.civicdesk.module.serviceRequest.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDateTime;

/**
 * Body for POST /civicDesk/serviceRequest/getServiceRequestAnalytics.
 */
public record ServiceRequestAnalyticsRequest(
        LocalDateTime fromDate,
        LocalDateTime toDate,
        @JsonAlias({"departmentId"}) String deptId
) {
}
