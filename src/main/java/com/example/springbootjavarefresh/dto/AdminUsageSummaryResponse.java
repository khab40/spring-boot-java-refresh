package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.ApiKeyUsageRecord;
import com.example.springbootjavarefresh.entity.UsageType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminUsageSummaryResponse(
        Long id,
        Long userId,
        String userEmail,
        Long productId,
        String productCode,
        UsageType usageType,
        BigDecimal megabytesUsed,
        Long payloadKilobytesUsed,
        Integer realtimeSubscriptionsUsed,
        Integer requestCount,
        String notes,
        LocalDateTime occurredAt) {

    public static AdminUsageSummaryResponse fromUsage(ApiKeyUsageRecord usageRecord) {
        return new AdminUsageSummaryResponse(
                usageRecord.getId(),
                usageRecord.getUser().getId(),
                usageRecord.getUser().getEmail(),
                usageRecord.getProduct().getId(),
                usageRecord.getProduct().getCode(),
                usageRecord.getUsageType(),
                usageRecord.getMegabytesUsed(),
                usageRecord.getPayloadKilobytesUsed(),
                usageRecord.getRealtimeSubscriptionsUsed(),
                usageRecord.getRequestCount(),
                usageRecord.getNotes(),
                usageRecord.getOccurredAt()
        );
    }
}
