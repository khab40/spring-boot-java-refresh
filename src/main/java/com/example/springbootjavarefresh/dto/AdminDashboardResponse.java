package com.example.springbootjavarefresh.dto;

import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalProducts,
        long totalPayments,
        long totalApiKeys,
        long totalUsageRecords,
        long activeEntitlements,
        List<UserProfileResponse> recentUsers,
        List<AdminPaymentSummaryResponse> recentPayments,
        List<AdminUsageSummaryResponse> recentUsage,
        List<AdminEntitlementSummaryResponse> recentEntitlements) {
}
