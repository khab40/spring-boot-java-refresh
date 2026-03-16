package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.EntitlementStatus;
import com.example.springbootjavarefresh.entity.ProductAccessType;
import com.example.springbootjavarefresh.entity.UserEntitlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminEntitlementSummaryResponse(
        Long id,
        Long userId,
        String userEmail,
        Long productId,
        String productCode,
        ProductAccessType accessType,
        EntitlementStatus status,
        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        BigDecimal batchDownloadUsedMb,
        Integer realtimeSubscriptionsUsed,
        Long payloadKilobytesUsed) {

    public static AdminEntitlementSummaryResponse fromEntitlement(UserEntitlement entitlement) {
        return new AdminEntitlementSummaryResponse(
                entitlement.getId(),
                entitlement.getUser().getId(),
                entitlement.getUser().getEmail(),
                entitlement.getProduct().getId(),
                entitlement.getProduct().getCode(),
                entitlement.getAccessType(),
                entitlement.getStatus(),
                entitlement.getGrantedAt(),
                entitlement.getExpiresAt(),
                entitlement.getBatchDownloadUsedMb(),
                entitlement.getRealtimeSubscriptionsUsed(),
                entitlement.getPayloadKilobytesUsed()
        );
    }
}
