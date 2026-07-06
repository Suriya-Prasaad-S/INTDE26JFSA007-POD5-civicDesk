package com.civicdesk.module.permit.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PermitAnalyticsResponse {

    private long totalPermits;

    private List<AnalyticsLabelCountDto> statusBreakdown;

    private List<AnalyticsLabelCountDto> permitTypeBreakdown;

    private List<AnalyticsTrendResponse> applicationTrend;

    private List<AnalyticsTrendResponse> decisionTrend;

    private Double averageDecisionDays;

    private InspectionAnalytics inspection;

    @Getter
    @Setter
    public static class InspectionAnalytics {

        private List<AnalyticsLabelCountDto> statusBreakdown;

        private List<AnalyticsLabelCountDto> outcomeBreakdown;
    }
}