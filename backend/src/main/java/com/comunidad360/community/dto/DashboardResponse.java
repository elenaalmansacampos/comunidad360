package com.comunidad360.community.dto;

import java.util.List;

public record DashboardResponse(
        CommunitySummaryResponse community,
        List<String> roles,
        long activeMembers,
        long openIncidents,
        DashboardFinanceResponse finance,
        List<DashboardIncidentResponse> recentIncidents,
        List<DashboardMeetingResponse> upcomingMeetings,
        List<DashboardMonthResponse> monthlyFinance,
        boolean canViewReports
) {
}
