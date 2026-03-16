package com.example.springbootjavarefresh.dto;

import java.math.BigDecimal;

public record ApiKeyUsageSummaryResponse(
        Long userId,
        Long productId,
        BigDecimal batchDownloadUsedMb,
        BigDecimal batchDownloadRemainingMb,
        Integer realtimeSubscriptionsUsed,
        Integer realtimeSubscriptionsRemaining,
        Long payloadKilobytesUsed,
        Long payloadKilobytesRemaining) {
}
