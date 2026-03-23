package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.UserEntitlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserEntitlementResponse(
        Long id,
        Object accessType,
        Object status,
        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        Integer purchasedUnits,
        BigDecimal batchDownloadUsedMb,
        Integer realtimeSubscriptionsUsed,
        Long payloadKilobytesUsed,
        DataProductResponse product) {

    public static UserEntitlementResponse fromEntitlement(UserEntitlement entitlement) {
        return new UserEntitlementResponse(
                entitlement.getId(),
                entitlement.getAccessType(),
                entitlement.getStatus(),
                entitlement.getGrantedAt(),
                entitlement.getExpiresAt(),
                entitlement.getPurchasedUnits(),
                entitlement.getBatchDownloadUsedMb(),
                entitlement.getRealtimeSubscriptionsUsed(),
                entitlement.getPayloadKilobytesUsed(),
                DataProductResponse.fromProduct(entitlement.getProduct())
        );
    }
}
